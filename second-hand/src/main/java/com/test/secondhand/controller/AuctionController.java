package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.dto.AuctionBidDTO;
import com.test.secondhand.dto.AuctionPublishDTO;
import com.test.secondhand.entity.AuctionGoods;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.AuctionService;
import com.test.secondhand.service.GoodsService;
import com.test.secondhand.vo.AuctionGoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private GoodsService goodsService;

    /**
     * 获取所有拍卖商品列表
     */
    @GetMapping("/list")
    public Result<List<AuctionGoodsVO>> list() {
        return Result.success(auctionService.getAllAuctionGoodsList());
    }

    /**
     * 获取特定拍卖商品详情
     */
    @GetMapping("/detail/{goodsId}")
    public Result<AuctionGoodsVO> detail(@PathVariable Long goodsId) {
        return Result.success(auctionService.getAuctionDetail(goodsId));
    }

    /**
     * 发布商品进行拍卖
     */
    @PostMapping("/publish")
    @FastAuthorize(required = true)
    public Result<?> publish(@RequestBody AuctionPublishDTO req) {
        Goods goods = goodsService.getGoodsById(req.getGoodsId());
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (!goods.getSellerId().equals(UserContext.getUserId())) {
            return Result.error("只能将自己发布的二手商品进行拍卖");
        }

        AuctionGoods ag = new AuctionGoods();
        ag.setGoodsId(req.getGoodsId());
        ag.setStartPrice(req.getStartPrice());
        ag.setCurrentPrice(BigDecimal.ZERO);
        ag.setMinIncrement(req.getMinIncrement());
        ag.setStartTime(req.getStartTime());
        ag.setEndTime(req.getEndTime());
        ag.setStatus(0);
        ag.setCreateTime(LocalDateTime.now());
        auctionService.publishAuction(ag);

        return Result.success();
    }

    /**
     * 用户出价接口，限制 2 秒内只能点 1 次，防止机器人快速刷出价
     */
    @PostMapping("/bid")
    @FastAuthorize(required = true, limitSeconds = 2, maxRequests = 1)
    public Result<?> placeBid(@RequestBody AuctionBidDTO req) {
        Long userId = UserContext.getUserId();
        auctionService.placeBid(req.getAuctionGoodsId(), userId, req.getBidPrice());
        return Result.success("出价成功！");
    }
}
