package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.FavoriteService;
import com.test.secondhand.service.GoodsService;
import com.test.secondhand.service.ReviewService;
import com.test.secondhand.service.UserService;
import com.test.secondhand.vo.GoodsDetailVO;
import com.test.secondhand.vo.GoodsVO;
import com.test.secondhand.vo.UserPublicVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private com.test.secondhand.service.OrderService orderService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/list")
    public Result<List<GoodsVO>> list() {
        List<Goods> goodsList = goodsService.getActiveGoodsList();
        List<GoodsVO> voList = goodsList.stream()
                .map(GoodsVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @GetMapping("/search")
    public Result<List<GoodsVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Goods> goodsList = goodsService.searchGoods(keyword, category, sortBy, page, size);
        List<GoodsVO> voList = goodsList.stream()
                .map(GoodsVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @GetMapping("/sync")
    public Result<String> sync() {
        int count = goodsService.syncAllGoodsToEs();
        return Result.success("成功全量同步 " + count + " 个商品到 Elasticsearch 向量检索库！");
    }

    @GetMapping("/detail/{id}")
    public Result<GoodsDetailVO> detail(@PathVariable Long id) {
        Goods goods = goodsService.getGoodsById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        // 增加浏览量
        goodsService.incrementViewCount(id);

        GoodsDetailVO detailVO = new GoodsDetailVO();
        detailVO.setGoods(GoodsVO.from(goods));

        // 卖家信息
        Map<String, Object> sellerInfo = userService.getUserPublicInfo(goods.getSellerId());
        UserPublicVO sellerVO = new UserPublicVO();
        sellerVO.setId((Long) sellerInfo.get("id"));
        sellerVO.setNickname((String) sellerInfo.get("nickname"));
        sellerVO.setAvatar((String) sellerInfo.get("avatar"));
        sellerVO.setBio((String) sellerInfo.get("bio"));
        sellerVO.setCreateTime((java.time.LocalDateTime) sellerInfo.get("createTime"));
        detailVO.setSeller(sellerVO);

        // 卖家评价统计
        Map<String, Object> reviewStats = reviewService.getUserReviewStats(goods.getSellerId());
        detailVO.setReviewStats(reviewStats);

        // 当前用户是否已收藏（未登录返回false）
        Long currentUserId = null;
        try {
            currentUserId = UserContext.getUserId();
        } catch (Exception e) {
            // 未登录
        }
        boolean isFavorite = favoriteService.isFavorite(currentUserId, id);
        detailVO.setIsFavorite(isFavorite);

        // 收藏数
        int favoriteCount = favoriteService.getFavoriteCount(id);
        detailVO.setFavoriteCount(favoriteCount);

        // 相关推荐
        List<Goods> relatedGoods = goodsService.getRelatedGoods(id, goods.getCategory(), 5);
        List<GoodsVO> relatedVOList = relatedGoods.stream()
                .map(GoodsVO::from)
                .collect(Collectors.toList());
        detailVO.setRelatedGoods(relatedVOList);

        return Result.success(detailVO);
    }

    @PostMapping("/publish")
    @FastAuthorize(required = true)
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
