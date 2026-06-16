package com.test.secondhand.service;

import com.test.secondhand.entity.Goods;

import java.util.List;

/**
 * Elasticsearch 服务接口
 */
public interface ElasticsearchService {

    /**
     * 初始化索引
     */
    void initIndex();

    /**
     * 同步商品数据到 Elasticsearch
     */
    void syncGoods(Goods goods);

    /**
     * 从 Elasticsearch 删除商品
     */
    void deleteGoods(Long goodsId);

    /**
     * 混合搜索：结合 BM25 关键字匹配与 kNN 向量语义相似度搜索
     */
    List<Long> searchGoodsIds(String keyword, String category, String sortBy, int page, int size);
}
