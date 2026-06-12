package com.test.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.AuctionGoods;
import com.test.secondhand.mapper.AuctionGoodsMapper;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.AuctionService;
import com.test.secondhand.service.GoodsService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionGoodsMapper auctionGoodsMapper;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public static class AuctionGoodsVO {
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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public void setGoodsId(Long goodsId) {
            this.goodsId = goodsId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public BigDecimal getStartPrice() {
            return startPrice;
        }

        public void setStartPrice(BigDecimal startPrice) {
            this.startPrice = startPrice;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public Long getHighestBidderId() {
            return highestBidderId;
        }

        public void setHighestBidderId(Long highestBidderId) {
            this.highestBidderId = highestBidderId;
        }

        public BigDecimal getMinIncrement() {
            return minIncrement;
        }

        public void setMinIncrement(BigDecimal minIncrement) {
            this.minIncrement = minIncrement;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Boolean getStarted() {
            return started;
        }

        public void setStarted(Boolean started) {
            this.started = started;
        }

        public Boolean getEnded() {
            return ended;
        }

        public void setEnded(Boolean ended) {
            this.ended = ended;
        }
    }

    /**
     * 获取所有拍卖商品列表
     */
    @GetMapping("/list")
    public Result<List<AuctionGoodsVO>> list() {
        List<AuctionGoods> list = auctionGoodsMapper.selectList(new LambdaQueryWrapper<>());
        List<AuctionGoodsVO> voList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AuctionGoods ag : list) {
            Goods goods = goodsService.getGoodsById(ag.getGoodsId());
            if (goods == null) continue;

            // 优先从 Redis 获取最新的最高出价，提升读取性能
            String cachedPrice = redisTemplate.opsForValue().get("auction:price:" + ag.getId());
            BigDecimal currentPrice = cachedPrice != null ? new BigDecimal(cachedPrice) : ag.getCurrentPrice();

            AuctionGoodsVO vo = new AuctionGoodsVO();
            vo.setId(ag.getId());
            vo.setGoodsId(ag.getGoodsId());
            vo.setName(goods.getName());
            vo.setDescription(goods.getDescription());
            vo.setImageUrl(goods.getImageUrl());
            vo.setStartPrice(ag.getStartPrice());
            vo.setCurrentPrice(currentPrice);
            vo.setHighestBidderId(ag.getHighestBidderId());
            vo.setMinIncrement(ag.getMinIncrement());
            vo.setStartTime(ag.getStartTime());
            vo.setEndTime(ag.getEndTime());
            vo.setStatus(ag.getStatus());
            vo.setStarted(now.isAfter(ag.getStartTime()));
            vo.setEnded(now.isAfter(ag.getEndTime()));
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /**
     * 获取特定拍卖商品详情
     */
    @GetMapping("/detail/{goodsId}")
    public Result<AuctionGoodsVO> detail(@PathVariable Long goodsId) {
        AuctionGoods ag = auctionGoodsMapper.selectOne(
                new LambdaQueryWrapper<AuctionGoods>().eq(AuctionGoods::getGoodsId, goodsId));
        if (ag == null) {
            return Result.error("拍卖商品不存在");
        }
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        // 优先从 Redis 读取高频变动的最高出价
        String cachedPrice = redisTemplate.opsForValue().get("auction:price:" + ag.getId());
        BigDecimal currentPrice = cachedPrice != null ? new BigDecimal(cachedPrice) : ag.getCurrentPrice();

        LocalDateTime now = LocalDateTime.now();
        AuctionGoodsVO vo = new AuctionGoodsVO();
        vo.setId(ag.getId());
        vo.setGoodsId(ag.getGoodsId());
        vo.setName(goods.getName());
        vo.setDescription(goods.getDescription());
        vo.setImageUrl(goods.getImageUrl());
        vo.setStartPrice(ag.getStartPrice());
        vo.setCurrentPrice(currentPrice);
        vo.setHighestBidderId(ag.getHighestBidderId());
        vo.setMinIncrement(ag.getMinIncrement());
        vo.setStartTime(ag.getStartTime());
        vo.setEndTime(ag.getEndTime());
        vo.setStatus(ag.getStatus());
        vo.setStarted(now.isAfter(ag.getStartTime()));
        vo.setEnded(now.isAfter(ag.getEndTime()));

        return Result.success(vo);
    }

    /**
     * 发布商品进行拍卖
     */
    @PostMapping("/publish")
    @FastAuthorize(required = true)
    public Result<?> publish(@RequestBody Map<String, Object> req) {
        Long goodsId = Long.valueOf(req.get("goodsId").toString());
        BigDecimal startPrice = new BigDecimal(req.get("startPrice").toString());
        BigDecimal minIncrement = new BigDecimal(req.get("minIncrement").toString());
        LocalDateTime startTime = LocalDateTime.parse(req.get("startTime").toString());
        LocalDateTime endTime = LocalDateTime.parse(req.get("endTime").toString());

        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (!goods.getSellerId().equals(UserContext.getUserId())) {
            return Result.error("只能将自己发布的二手商品进行拍卖");
        }

        AuctionGoods ag = new AuctionGoods();
        ag.setGoodsId(goodsId);
        ag.setStartPrice(startPrice);
        ag.setCurrentPrice(BigDecimal.ZERO); // 初始尚无出价
        ag.setMinIncrement(minIncrement);
        ag.setStartTime(startTime);
        ag.setEndTime(endTime);
        ag.setStatus(0); // 竞拍中
        ag.setCreateTime(LocalDateTime.now());
        auctionGoodsMapper.insert(ag);

        return Result.success();
    }

    /**
     * 用户出价接口，限制 2 秒内只能点 1 次，防止机器人快速刷出价
     */
    @PostMapping("/bid")
    @FastAuthorize(required = true, limitSeconds = 2, maxRequests = 1)
    public Result<?> placeBid(@RequestBody Map<String, Object> req) {
        Long auctionGoodsId = Long.valueOf(req.get("auctionGoodsId").toString());
        BigDecimal bidPrice = new BigDecimal(req.get("bidPrice").toString());
        Long userId = UserContext.getUserId();

        auctionService.placeBid(auctionGoodsId, userId, bidPrice);
        return Result.success("出价成功！");
    }
}
