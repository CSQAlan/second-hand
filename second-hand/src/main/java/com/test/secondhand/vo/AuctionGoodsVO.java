package com.test.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拍卖商品视图对象
 */
@Data
public class AuctionGoodsVO {

    private Long id;
    private Long goodsId;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private Long highestBidderId;
    private BigDecimal minIncrement;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0-竞拍中, 1-已结束
    private Boolean started;
    private Boolean ended;
}
