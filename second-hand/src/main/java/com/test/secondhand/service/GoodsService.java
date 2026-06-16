package com.test.secondhand.service;

import com.test.secondhand.entity.Goods;

import java.util.List;

/**
 * 商品服务接口
 */
public interface GoodsService {

    /**
     * 获取全部商品列表（在售状态）
     */
    List<Goods> getActiveGoodsList();

    /**
     * 根据ID获取商品详情
     */
    Goods getGoodsById(Long id);

    /**
     * 发布二手商品
     */
    void publishGoods(Goods goods);

    /**
     * 更新商品信息
     */
    void updateGoods(Goods goods);

    /**
     * 将数据库中所有的在售商品全量同步到 Elasticsearch
     */
    int syncAllGoodsToEs();

    /**
     * 商品搜索
     */
    List<Goods> searchGoods(String keyword, String category, String sortBy, int page, int size);

    /**
     * 增加浏览量
     */
    void incrementViewCount(Long goodsId);

    /**
     * 获取相关推荐商品
     */
    List<Goods> getRelatedGoods(Long goodsId, String category, int limit);
}
