package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Review;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.ReviewService;
import com.test.secondhand.vo.ReviewStatsVO;
import com.test.secondhand.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Result<List<ReviewVO>> getUserReviews(@PathVariable Long userId) {
        List<Review> reviews = reviewService.getUserReviews(userId);
        List<ReviewVO> voList = reviews.stream()
                .map(ReviewVO::from)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 获取用户评价统计
     */
    @GetMapping("/stats/{userId}")
    public Result<ReviewStatsVO> getUserReviewStats(@PathVariable Long userId) {
        Map<String, Object> stats = reviewService.getUserReviewStats(userId);

        ReviewStatsVO vo = new ReviewStatsVO();
        vo.setAverageRating((Double) stats.get("averageRating"));
        vo.setTotalReviews((Integer) stats.get("totalReviews"));
        vo.setGoodRate((Integer) stats.get("goodRate"));
        vo.setRatingDistribution((Map<Integer, Long>) stats.get("ratingDistribution"));

        return Result.success(vo);
    }

    /**
     * 获取订单的评价
     */
    @GetMapping("/order/{orderId}")
    public Result<?> getOrderReview(@PathVariable Long orderId) {
        // 获取买家评价和卖家评价
        Review buyerReview = reviewService.getOrderReview(orderId, 1);
        Review sellerReview = reviewService.getOrderReview(orderId, 2);

        Map<String, ReviewVO> result = Map.of(
                "buyerReview", ReviewVO.from(buyerReview),
                "sellerReview", ReviewVO.from(sellerReview));
        return Result.success(result);
    }
}
