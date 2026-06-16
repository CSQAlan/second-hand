package com.test.secondhand.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long goodsId;
    private String goodsName;
    private String goodsImageUrl;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal price;
    private Integer type; // 0-普通购买, 1-秒杀抢购, 2-拍卖成交
    private Integer status; // 0-待付款, 1-已付款, 2-已发货, 3-已收货, 4-交易成功, 5-已取消
    private String deliveryNo;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
}
