package com.test.secondhand.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("auction_record")
public class AuctionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long auctionGoodsId;
    private Long bidderId;
    private BigDecimal bidPrice;
    private LocalDateTime bidTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionGoodsId() {
        return auctionGoodsId;
    }

    public void setAuctionGoodsId(Long auctionGoodsId) {
        this.auctionGoodsId = auctionGoodsId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
