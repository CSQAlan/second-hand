package com.test.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Order;
import com.test.secondhand.entity.Review;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.mapper.ReviewMapper;
import com.test.secondhand.service.OrderService;
import com.test.secondhand.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Override
    public void createReview(Long reviewerId, Review review) {
        // 验证订单存在
        Order order = orderMapper.selectById(review.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 验证订单状态（必须是交易成功或已收货状态）
        if (order.getStatus() != 3 && order.getStatus() != 4) {
            throw new BusinessException("订单状态不允许评价");
        }

        // 验证评价类型和权限
        if (review.getType() == 1) {
            // 买家评卖家
            if (!order.getBuyerId().equals(reviewerId)) {
                throw new BusinessException("只有买家才能评价卖家");
            }
            review.setTargetUserId(order.getSellerId());
        } else if (review.getType() == 2) {
            // 卖家评买家
            if (!order.getSellerId().equals(reviewerId)) {
                throw new BusinessException("只有卖家才能评价买家");
            }
            review.setTargetUserId(order.getBuyerId());
        } else {
            throw new BusinessException("评价类型无效");
        }

        // 检查是否已评价
        if (hasReviewed(order.getId(), review.getType())) {
            throw new BusinessException("已评价过该订单");
        }

        // 验证评分范围
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        review.setOrderId(order.getId());
        review.setReviewerId(reviewerId);
        review.setGoodsId(order.getGoodsId());
        review.setCreateTime(LocalDateTime.now());

        reviewMapper.insert(review);

        // 评价后如果订单状态是已收货(3)，则自动完成交易(4)
        if (order.getStatus() == 3) {
            orderService.completeOrder(order.getOrderNo());
        }
    }

    @Override
    public List<Review> getUserReviews(Long userId) {
        return reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetUserId, userId)
                        .orderByDesc(Review::getCreateTime));
    }

    @Override
    public Map<String, Object> getUserReviewStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTargetUserId, userId));

        if (reviews.isEmpty()) {
            stats.put("averageRating", 0.0);
            stats.put("totalReviews", 0);
            stats.put("goodRate", 0);
            stats.put("ratingDistribution", Map.of(1, 0, 2, 0, 3, 0, 4, 0, 5, 0));
            return stats;
        }

        // 计算平均分
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        stats.put("averageRating", BigDecimal.valueOf(averageRating)
                .setScale(1, RoundingMode.HALF_UP).doubleValue());

        // 总评价数
        stats.put("totalReviews", reviews.size());

        // 好评率（4-5星为好评）
        long goodCount = reviews.stream()
                .filter(r -> r.getRating() >= 4)
                .count();
        int goodRate = (int) (goodCount * 100 / reviews.size());
        stats.put("goodRate", goodRate);

        // 评分分布
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            long count = reviews.stream()
                    .filter(r -> r.getRating() == star)
                    .count();
            distribution.put(star, count);
        }
        stats.put("ratingDistribution", distribution);

        return stats;
    }

    @Override
    public boolean hasReviewed(Long orderId, Integer type) {
        return reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getType, type)) > 0;
    }

    @Override
    public Review getOrderReview(Long orderId, Integer type) {
        return reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getType, type));
    }
}
