package com.test.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布秒杀商品请求 DTO
 */
@Data
public class SeckillPublishDTO {

    private Long goodsId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
