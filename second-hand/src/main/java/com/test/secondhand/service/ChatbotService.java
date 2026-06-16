package com.test.secondhand.service;

/**
 * 智能客服服务接口
 */
public interface ChatbotService {

    /**
     * 智能客服问答逻辑：本地 RAG 召回 + DeepSeek LLM 润色
     */
    String getChatResponse(String userQuestion);
}
