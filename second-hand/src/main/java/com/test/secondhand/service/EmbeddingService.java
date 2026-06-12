package com.test.secondhand.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${embedding.api-key:your-api-key-here}")
    private String apiKey;

    @Value("${embedding.api-url:https://api.openai.com/v1/embeddings}")
    private String apiUrl;

    @Value("${embedding.model:text-embedding-3-small}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取文本的 1536 维 Embedding 向量
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return getMockEmbedding("");
        }

        // 检查是否配置了有效的 API Key
        if ("your-api-key-here".equalsIgnoreCase(apiKey) || apiKey.trim().isEmpty()) {
            log.debug("[Embedding] API Key 未配置，降级为本地确定性 mock 向量。文本: {}", text);
            return getMockEmbedding(text);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", modelName);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.path("data").get(0).path("embedding");
                if (embeddingNode.isArray()) {
                    float[] vector = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vector[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    log.info("[Embedding] 成功获取远程 Embedding 向量，维度: {}", vector.length);
                    return vector;
                }
            }
            log.warn("[Embedding] 远程获取失败，状态码: {}，转本地降级回答", response.getStatusCode());
            return getMockEmbedding(text);

        } catch (Exception e) {
            log.error("[Embedding] 联网调用 Embedding API 异常，降级为本地确定性 mock 向量", e);
            return getMockEmbedding(text);
        }
    }

    /**
     * 生成确定性的、单位化的 1536 维模拟 Embedding 向量。
     * 使用文本的哈希值作为随机种子，保证相同的文本总是能生成相同的向量，方便无网络状态下本地开发和测试。
     */
    public float[] getMockEmbedding(String text) {
        int dimension = 1536;
        float[] vector = new float[dimension];
        long seed = (text == null) ? 0 : text.hashCode();
        
        Random random = new Random(seed);
        float sumOfSquares = 0;
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat() * 2 - 1; // 范围 [-1, 1]
            sumOfSquares += vector[i] * vector[i];
        }

        // 单位归一化（使余弦相似度等价于点积）
        float magnitude = (float) Math.sqrt(sumOfSquares);
        if (magnitude > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] /= magnitude;
            }
        }
        return vector;
    }
}
