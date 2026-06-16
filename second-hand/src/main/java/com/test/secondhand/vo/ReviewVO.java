package com.test.secondhand.vo;

import com.test.secondhand.entity.Review;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价视图对象
 */
@Data
public class ReviewVO {

    private Long id;
    private Long orderId;
    private Long reviewerId;
    private Long targetUserId;
    private Long goodsId;
    private Integer rating;
    private String content;
    private String images;
    private Integer type;
    private LocalDateTime createTime;

    /**
     * 从 Entity 转换为 VO
     */
    public static ReviewVO from(Review review) {
        if (review == null) {
            return null;
        }
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setOrderId(review.getOrderId());
        vo.setReviewerId(review.getReviewerId());
        vo.setTargetUserId(review.getTargetUserId());
        vo.setGoodsId(review.getGoodsId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(review.getImages());
        vo.setType(review.getType());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }
}
