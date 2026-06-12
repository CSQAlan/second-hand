package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private com.test.secondhand.service.OrderService orderService;

    @Autowired
    private com.test.secondhand.service.FavoriteService favoriteService;

    @Autowired
    private com.test.secondhand.service.UserService userService;

    @Autowired
    private com.test.secondhand.service.ReviewService reviewService;

    @GetMapping("/list")
    public Result<List<Goods>> list() {
        return Result.success(goodsService.getActiveGoodsList());
    }

    @GetMapping("/search")
    public Result<List<Goods>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(goodsService.searchGoods(keyword, category, sortBy, page, size));
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Goods goods = goodsService.getGoodsById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        // 增加浏览量
        goodsService.incrementViewCount(id);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("goods", goods);

        // 卖家信息
        Map<String, Object> sellerInfo = userService.getUserPublicInfo(goods.getSellerId());
        // 卖家评价统计
        Map<String, Object> reviewStats = reviewService.getUserReviewStats(goods.getSellerId());
        sellerInfo.put("reviewStats", reviewStats);
        result.put("seller", sellerInfo);

        // 当前用户是否已收藏（未登录返回false）
        Long currentUserId = null;
        try {
            currentUserId = UserContext.getUserId();
        } catch (Exception e) {
            // 未登录
        }
        boolean isFavorite = favoriteService.isFavorite(currentUserId, id);
        result.put("isFavorite", isFavorite);

        // 收藏数
        int favoriteCount = favoriteService.getFavoriteCount(id);
        result.put("favoriteCount", favoriteCount);

        // 相关推荐
        List<Goods> relatedGoods = goodsService.getRelatedGoods(id, goods.getCategory(), 5);
        result.put("relatedGoods", relatedGoods);

        return Result.success(result);
    }

    @PostMapping("/publish")
    @FastAuthorize(required = true) // 使用自定义轻量级 AOP 鉴权，要求登录
    public Result<?> publish(@RequestBody Goods goods) {
        goods.setSellerId(UserContext.getUserId());
        goods.setCreateTime(LocalDateTime.now());
        goods.setUpdateTime(LocalDateTime.now());
        goodsService.publishGoods(goods);
        return Result.success();
    }

    @PutMapping("/update")
    @FastAuthorize(required = true)
    public Result<?> update(@RequestBody Goods goods) {
        Goods existing = goodsService.getGoodsById(goods.getId());
        if (existing == null) {
            return Result.error("商品不存在");
        }
        // 校验是否为原发布者
        if (!existing.getSellerId().equals(UserContext.getUserId()) && !"ROLE_ADMIN".equals(UserContext.getRole())) {
            return Result.error(403, "没有修改该商品的权限");
        }
        
        goods.setSellerId(existing.getSellerId());
        goods.setUpdateTime(LocalDateTime.now());
        goodsService.updateGoods(goods);
        return Result.success();
    }

    @PostMapping("/buy/{id}")
    @FastAuthorize(required = true)
    public Result<?> buyGoods(@PathVariable Long id) {
        try {
            Long buyerId = UserContext.getUserId();
            orderService.createOrder(id, buyerId);
            return Result.success("购买成功！");
        } catch (com.test.secondhand.exception.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("购买失败，系统异常");
        }
    }
}
