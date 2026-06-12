package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 收藏商品
     */
    @PostMapping("/{goodsId}")
    @FastAuthorize(required = true)
    public Result<?> addFavorite(@PathVariable Long goodsId) {
        Long userId = UserContext.getUserId();
        favoriteService.addFavorite(userId, goodsId);
        return Result.success("收藏成功");
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{goodsId}")
    @FastAuthorize(required = true)
    public Result<?> removeFavorite(@PathVariable Long goodsId) {
        Long userId = UserContext.getUserId();
        favoriteService.removeFavorite(userId, goodsId);
        return Result.success("取消收藏成功");
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check/{goodsId}")
    @FastAuthorize(required = true)
    public Result<Boolean> checkFavorite(@PathVariable Long goodsId) {
        Long userId = UserContext.getUserId();
        return Result.success(favoriteService.isFavorite(userId, goodsId));
    }

    /**
     * 获取我的收藏列表
     */
    @GetMapping("/list")
    @FastAuthorize(required = true)
    public Result<List<Goods>> getFavorites() {
        Long userId = UserContext.getUserId();
        return Result.success(favoriteService.getUserFavorites(userId));
    }
}
