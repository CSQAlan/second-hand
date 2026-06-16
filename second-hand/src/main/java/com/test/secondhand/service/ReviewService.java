package com.test.secondhand.service;

import com.test.secondhand.entity.Review;

import java.util.List;
import java.util.Map;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 发表评价
     */
    void createReview(Long reviewerId, Review review);

    /**
     * 获取用户收到的评价
     */
    List<Review> getUserReviews(Long userId);

    /**
     * 获取用户评价统计
     */
    Map<String, Object> getUserReviewStats(Long userId);

    /**
     * 检查订单是否已评价
     */
    boolean hasReviewed(Long orderId, Integer type);

    /**
     * 获取订单的评价
     */
    Review getOrderReview(Long orderId, Integer type);
}
