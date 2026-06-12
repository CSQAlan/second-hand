package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Favorite;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.FavoriteMapper;
import com.test.secondhand.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 收藏商品
     */
    public void addFavorite(Long userId, Long goodsId) {
        // 检查商品是否存在
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查是否已收藏
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getGoodsId, goodsId));
        if (existing != null) {
            throw new BusinessException("已收藏该商品");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(goodsId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
    }

    /**
     * 取消收藏
     */
    public void removeFavorite(Long userId, Long goodsId) {
        int deleted = favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getGoodsId, goodsId));
        if (deleted == 0) {
            throw new BusinessException("未收藏该商品");
        }
    }

    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long userId, Long goodsId) {
        if (userId == null) {
            return false;
        }
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getGoodsId, goodsId)) > 0;
    }

    /**
     * 获取用户收藏列表
     */
    public List<Goods> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> goodsIds = favorites.stream()
                .map(Favorite::getGoodsId)
                .toList();

        return goodsMapper.selectBatchIds(goodsIds);
    }

    /**
     * 获取商品收藏数
     */
    public int getFavoriteCount(Long goodsId) {
        return Math.toIntExact(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getGoodsId, goodsId)));
    }
}
