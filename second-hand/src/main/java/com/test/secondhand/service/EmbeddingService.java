package com.test.secondhand.service;

/**
 * Embedding 向量服务接口
 */
public interface EmbeddingService {

    /**
     * 获取文本的 Embedding 向量
     */
    float[] getEmbedding(String text);

    /**
     * 生成确定性的、单位化的模拟 Embedding 向量
     */
    float[] getMockEmbedding(String text);
}
