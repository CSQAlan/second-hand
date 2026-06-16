package com.test.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布拍卖商品请求 DTO
 */
@Data
public class AuctionPublishDTO {

    private Long goodsId;
    private BigDecimal startPrice;
    private BigDecimal minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
