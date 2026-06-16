package com.test.secondhand.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品详情视图对象（包含卖家信息、收藏状态等）
 */
@Data
public class GoodsDetailVO {

    private GoodsVO goods;
    private UserPublicVO seller;
    private Object reviewStats;
    private Boolean isFavorite;
    private Integer favoriteCount;
    private List<GoodsVO> relatedGoods;
}
