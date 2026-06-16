package com.test.secondhand.service;

import com.test.secondhand.entity.Goods;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /**
     * 收藏商品
     */
    void addFavorite(Long userId, Long goodsId);

    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Long goodsId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorite(Long userId, Long goodsId);

    /**
     * 获取用户收藏列表
     */
    List<Goods> getUserFavorites(Long userId);

    /**
     * 获取商品收藏数
     */
    int getFavoriteCount(Long goodsId);
}
