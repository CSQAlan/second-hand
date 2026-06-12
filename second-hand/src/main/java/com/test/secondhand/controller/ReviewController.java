package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Review;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 发表评价
     */
    @PostMapping
    @FastAuthorize(required = true)
    public Result<?> createReview(@RequestBody Review review) {
        Long userId = UserContext.getUserId();
        reviewService.createReview(userId, review);
        return Result.success("评价成功");
    }

    /**
     * 获取用户收到的评价
     */
    @GetMapping("/user/{userId}")
    public Result<List<Review>> getUserReviews(@PathVariable Long userId) {
        return Result.success(reviewService.getUserReviews(userId));
    }

    /**
     * 获取用户评价统计
     */
    @GetMapping("/stats/{userId}")
    public Result<Map<String, Object>> getUserReviewStats(@PathVariable Long userId) {
        return Result.success(reviewService.getUserReviewStats(userId));
    }

    /**
     * 获取订单的评价
     */
    @GetMapping("/order/{orderId}")
    public Result<?> getOrderReview(@PathVariable Long orderId) {
        Long userId = UserContext.getUserId();
        // 获取买家评价和卖家评价
        Review buyerReview = reviewService.getOrderReview(orderId, 1);
        Review sellerReview = reviewService.getOrderReview(orderId, 2);

        Map<String, Review> result = Map.of(
                "buyerReview", buyerReview,
                "sellerReview", sellerReview);
        return Result.success(result);
    }
}
