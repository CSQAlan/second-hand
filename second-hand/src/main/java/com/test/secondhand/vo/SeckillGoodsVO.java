package com.test.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品视图对象
 */
@Data
public class SeckillGoodsVO {

    private Long id;
    private Long goodsId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean started;
    private Boolean ended;
}
