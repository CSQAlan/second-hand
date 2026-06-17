package com.test.secondhand.vo;

import com.test.secondhand.entity.Goods;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品视图对象
 */
@Data
public class GoodsVO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String images;
    private String category;
    private String condition;
    private String tradingMethod;
    private String location;
    private Integer viewCount;
    private Integer status;
    private Long sellerId;
    private LocalDateTime createTime;

    /**
     * 从 Entity 转换为 VO
     */
    public static GoodsVO from(Goods goods) {
        if (goods == null) {
            return null;
        }
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setName(goods.getName());
        vo.setDescription(goods.getDescription());
        vo.setPrice(goods.getPrice());
        vo.setImageUrl(goods.getImageUrl());
        vo.setImages(goods.getImages());
        vo.setCategory(goods.getCategory());
        vo.setCondition(goods.getCondition());
        vo.setTradingMethod(goods.getTradingMethod());
        vo.setLocation(goods.getLocation());
        vo.setViewCount(goods.getViewCount());
        vo.setStatus(goods.getStatus());
        vo.setSellerId(goods.getSellerId());
        vo.setCreateTime(goods.getCreateTime());
        return vo;
    }
}
