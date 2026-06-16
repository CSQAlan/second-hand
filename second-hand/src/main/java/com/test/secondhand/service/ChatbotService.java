package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.secondhand.entity.KnowledgeBase;
import com.test.secondhand.mapper.KnowledgeBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChatbotService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private GoodsService goodsService;

    @Value("${deepseek.api-key:your-api-key-here}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 智能客服问答逻辑：本地 RAG 召回 + DeepSeek LLM 润色
     */
    public String getChatResponse(String userQuestion) {
        log.info("[智能客服] 收到用户提问: {}", userQuestion);

        // 1. 本地知识检索 (Retrieval)：根据关键字匹配知识库
        List<KnowledgeBase> allKnowledge = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<>());
        StringBuilder contextBuilder = new StringBuilder();
        
        // 简单检索：检查问题是否包含预设的某些主题字眼
        for (KnowledgeBase kb : allKnowledge) {
            if (userQuestion.contains(kb.getQuestion()) 
                || (kb.getCategory() != null && userQuestion.toLowerCase().contains(kb.getCategory()))
                || containsAnyKeyword(userQuestion, kb.getQuestion().split("(?<=\\G.{2})"))) { // 字符片段匹配
                
                contextBuilder.append("问：").append(kb.getQuestion()).append("\n");
                contextBuilder.append("答：").append(kb.getAnswer()).append("\n\n");
            }
        }

        String context = contextBuilder.toString().trim();
        if (context.isEmpty()) {
            context = "暂无直接相关的参考规则。";
        }

        log.info("[智能客服] 本地检索召回的参考背景: \n{}", context);

        // 2. 检索商品数据库/ES，为 AI 提供实时在售商品信息
        List<com.test.secondhand.entity.Goods> matchedGoods = new ArrayList<>();
        try {
            // 通过语义或全文搜索，为客服检索关联商品
            matchedGoods = goodsService.searchGoods(userQuestion, null, "views", 1, 3);
        } catch (Exception e) {
            log.error("[智能客服] 关联在售商品检索异常", e);
        }

        StringBuilder goodsContextBuilder = new StringBuilder();
        if (matchedGoods != null && !matchedGoods.isEmpty()) {
            goodsContextBuilder.append("【实时系统在售商品推荐】:\n");
            for (com.test.secondhand.entity.Goods g : matchedGoods) {
                goodsContextBuilder.append("- <a href=\"/goods?detailId=").append(g.getId())
                        .append("\" class=\"chat-goods-link\">").append(g.getName()).append("</a>")
                        .append("，价格: ").append(g.getPrice()).append("元")
                        .append("，成色: ").append(g.getCondition() != null ? g.getCondition() : "未知")
                        .append("，卖家介绍: ").append(g.getDescription())
                        .append("\n");
            }
        }
        String goodsContext = goodsContextBuilder.toString();
        log.info("[智能客服] 关联召回的在售商品推荐背景: \n{}", goodsContext);

        // 3. 检查 API Key 是否有效。如果为默认值，则直接走本地规则和商品降级显示，防止调用报错
        if ("your-api-key-here".equalsIgnoreCase(apiKey) || apiKey.trim().isEmpty()) {
            log.warn("[智能客服] DeepSeek API Key 未配置，将降级为本地小助手模式");
            return formatFallbackResponse(context, goodsContext, userQuestion);
        }

        // 4. 构建 Prompt 并调用 DeepSeek 接口 (Generation)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            List<Map<String, String>> messages = new ArrayList<>();
            // System 角色输入背景知识及推荐商品信息
            String systemPrompt = "你是一个专业的二手交易系统智能客服助手。请结合下述系统规则背景知识和实时在售商品推荐，友好、精简、专业地回答用户的提问，并根据用户诉求向其推荐平台内的二手商品。\n\n" +
                    "【系统背景规则】\n" + context + "\n\n";
            if (!goodsContext.isEmpty()) {
                systemPrompt += goodsContext + "\n" +
                        "【商品推荐准则】：如果在售商品推荐中有与用户兴趣匹配的商品，请向用户极力推荐。你在回答中提到该商品时，必须直接把上述商品列表中的 HTML 链接（例如 <a href=\"/goods?detailId=8\" class=\"chat-goods-link\">商品名称</a>）直接复制到你的回答里，方便用户点击跳转！不要使用 Markdown 格式的链接！\n\n";
            }
            systemPrompt += "请注意：如果规则和商品中没有相关信息，请根据普通二手交易常识解答。字数保持在 250 字以内，生成的超链接标签必须完整且与提供的一致。";

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // User 角色输入用户提问
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userQuestion);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String answer = root.path("choices").get(0).path("message").path("content").asText();
                return answer.trim();
            } else {
                log.error("[智能客服] DeepSeek API 响应异常: {}", response.getStatusCode());
                return formatFallbackResponse(context, goodsContext, userQuestion);
            }

        } catch (Exception e) {
            log.error("[智能客服] 联网调用 DeepSeek 异常，转本地降级回答", e);
            return formatFallbackResponse(context, goodsContext, userQuestion);
        }
    }

    /**
     * 判断问题中是否包含某几个分词片段
     */
    private boolean containsAnyKeyword(String text, String[] keywords) {
        for (String kw : keywords) {
            if (kw.length() >= 2 && text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 本地规则及商品匹配降级解答格式化
     */
    private String formatFallbackResponse(String context, String goodsContext, String question) {
        StringBuilder response = new StringBuilder();
        if (context.contains("答：")) {
            response.append("【本地小助手提示：检测到当前处于本地解答模式】\n根据我们系统知识库的记录，相关的规则解答如下：\n\n").append(context).append("\n\n");
        } else {
            response.append("【本地小助手提示：未配置 AI 接口】\n抱歉，小助手没能找到关于“").append(question).append("”的系统匹配规则条目。\n\n");
        }
        
        if (goodsContext != null && !goodsContext.isEmpty()) {
            response.append("但我从平台商品库中为您匹配到了以下在售相关好物：\n").append(goodsContext);
        }
        return response.toString();
    }
}
