package com.test.secondhand.vo;

import lombok.Data;

import java.util.Map;

/**
 * 评价统计视图对象
 */
@Data
public class ReviewStatsVO {

    private Double averageRating;
    private Integer totalReviews;
    private Integer goodRate;
    private Map<Integer, Long> ratingDistribution;
}
