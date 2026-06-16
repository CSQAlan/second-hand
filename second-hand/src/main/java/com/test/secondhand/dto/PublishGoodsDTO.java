package com.test.secondhand.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布商品请求 DTO
 */
@Data
public class PublishGoodsDTO {

    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String images;
    private String category;
    private String condition;
    private String tradingMethod;
    private String location;
}
