package com.test.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拍卖出价请求 DTO
 */
@Data
public class AuctionBidDTO {

    private Long auctionGoodsId;
    private BigDecimal bidPrice;
}
