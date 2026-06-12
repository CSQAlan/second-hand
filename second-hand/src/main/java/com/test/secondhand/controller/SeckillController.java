package com.test.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.mapper.SeckillGoodsMapper;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.GoodsService;
import com.test.secondhand.service.SeckillService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private GoodsService goodsService;

    public static class SeckillGoodsVO {
        private Long id;
        private Long goodsId;
        private String name;
        private String description;
        private String imageUrl;
        private BigDecimal originalPrice;
        private BigDecimal seckillPrice;
        private Integer stock;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
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

        public BigDecimal getOriginalPrice() {
            return originalPrice;
        }

        public void setOriginalPrice(BigDecimal originalPrice) {
            this.originalPrice = originalPrice;
        }

        public BigDecimal getSeckillPrice() {
            return seckillPrice;
        }

        public void setSeckillPrice(BigDecimal seckillPrice) {
            this.seckillPrice = seckillPrice;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
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
     * 获取所有秒杀抢购商品列表
     */
    @GetMapping("/list")
    public Result<List<SeckillGoodsVO>> list() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(new LambdaQueryWrapper<>());
        List<SeckillGoodsVO> voList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (SeckillGoods sg : list) {
            Goods goods = goodsService.getGoodsById(sg.getGoodsId());
            if (goods == null) continue;

            SeckillGoodsVO vo = new SeckillGoodsVO();
            vo.setId(sg.getId());
            vo.setGoodsId(sg.getGoodsId());
            vo.setName(goods.getName());
            vo.setDescription(goods.getDescription());
            vo.setImageUrl(goods.getImageUrl());
            vo.setOriginalPrice(goods.getPrice());
            vo.setSeckillPrice(sg.getSeckillPrice());
            vo.setStock(sg.getStock());
            vo.setStartTime(sg.getStartTime());
            vo.setEndTime(sg.getEndTime());
            vo.setStarted(now.isAfter(sg.getStartTime()));
            vo.setEnded(now.isAfter(sg.getEndTime()));
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /**
     * 获取单个秒杀商品详情
     */
    @GetMapping("/detail/{goodsId}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long goodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId));
        if (sg == null) {
            return Result.error("秒杀商品不存在");
        }
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        SeckillGoodsVO vo = new SeckillGoodsVO();
        vo.setId(sg.getId());
        vo.setGoodsId(sg.getGoodsId());
        vo.setName(goods.getName());
        vo.setDescription(goods.getDescription());
        vo.setImageUrl(goods.getImageUrl());
        vo.setOriginalPrice(goods.getPrice());
        vo.setSeckillPrice(sg.getSeckillPrice());
        vo.setStock(sg.getStock());
        vo.setStartTime(sg.getStartTime());
        vo.setEndTime(sg.getEndTime());
        vo.setStarted(now.isAfter(sg.getStartTime()));
        vo.setEnded(now.isAfter(sg.getEndTime()));

        return Result.success(vo);
    }

    /**
     * 发布秒杀商品（通常由发布二手商品时或后台配置）
     */
    @PostMapping("/publish")
    @FastAuthorize(required = true)
    public Result<?> publish(@RequestBody Map<String, Object> req) {
        Long goodsId = Long.valueOf(req.get("goodsId").toString());
        BigDecimal seckillPrice = new BigDecimal(req.get("seckillPrice").toString());
        Integer stock = Integer.valueOf(req.get("stock").toString());
        LocalDateTime startTime = LocalDateTime.parse(req.get("startTime").toString());
        LocalDateTime endTime = LocalDateTime.parse(req.get("endTime").toString());

        // 校验是否是该商品的主人
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            return Result.error("二手商品不存在");
        }
        if (!goods.getSellerId().equals(UserContext.getUserId())) {
            return Result.error("只能将自己发布的二手商品转为秒杀抢购");
        }

        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setGoodsId(goodsId);
        seckillGoods.setSeckillPrice(seckillPrice);
        seckillGoods.setStock(stock);
        seckillGoods.setStartTime(startTime);
        seckillGoods.setEndTime(endTime);
        seckillGoods.setCreateTime(LocalDateTime.now());
        seckillGoodsMapper.insert(seckillGoods);

        // 自动进行库存预热，方便测试
        seckillService.warmUpStock(goodsId);

        return Result.success();
    }

    /**
     * 预热秒杀商品库存到 Redis
     */
    @PostMapping("/warmup/{goodsId}")
    public Result<?> warmup(@PathVariable Long goodsId) {
        seckillService.warmUpStock(goodsId);
        return Result.success("库存预热成功！");
    }

    /**
     * 高并发抢购下单入口
     * AOP 层面：登录校验 + 5秒限流1次，放掉 Spring Security 滤镜，保证吞吐量
     */
    @PostMapping("/order")
    @FastAuthorize(required = true, limitSeconds = 5, maxRequests = 1)
    public Result<?> placeOrder(@RequestBody Map<String, Object> req) {
        Long goodsId = Long.valueOf(req.get("goodsId").toString());
        Long userId = UserContext.getUserId();

        // 执行秒杀
        int result = seckillService.executeSeckill(goodsId, userId);
        
        switch (result) {
            case 1:
                return Result.success("抢购排队中，请稍后查看您的订单状态！");
            case -1:
                return Result.error("您已经秒杀过此商品，请勿重复抢购！");
            case -2:
                return Result.error("秒杀商品已售罄！");
            default:
                return Result.error("抢购失败，服务器异常");
        }
    }
}
