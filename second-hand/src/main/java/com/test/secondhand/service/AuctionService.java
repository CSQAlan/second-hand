package com.test.secondhand.service;

import com.test.secondhand.entity.AuctionGoods;
import com.test.secondhand.vo.AuctionGoodsVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拍卖服务接口
 */
public interface AuctionService {

    /**
     * 获取所有拍卖商品列表（含商品信息和实时出价）
     */
    List<AuctionGoodsVO> getAllAuctionGoodsList();

    /**
     * 获取单个拍卖商品详情（含商品信息和实时出价）
     */
    AuctionGoodsVO getAuctionDetail(Long goodsId);

    /**
     * 发布拍卖商品
     */
    void publishAuction(AuctionGoods auctionGoods);

    /**
     * 用户对拍卖商品进行出价
     */
    void placeBid(Long auctionGoodsId, Long userId, BigDecimal bidPrice);

    /**
     * 定时任务：自动结标已结束的拍卖
     */
    void closeEndedAuctions();
}
