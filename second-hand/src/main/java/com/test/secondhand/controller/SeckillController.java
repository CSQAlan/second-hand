package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.dto.SeckillPublishDTO;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.GoodsService;
import com.test.secondhand.service.SeckillService;
import com.test.secondhand.vo.SeckillGoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private GoodsService goodsService;

    /**
     * 获取所有秒杀抢购商品列表
     */
    @GetMapping("/list")
    public Result<List<SeckillGoodsVO>> list() {
        return Result.success(seckillService.getAllSeckillGoodsList());
    }

    /**
     * 获取单个秒杀商品详情
     */
    @GetMapping("/detail/{goodsId}")
    public Result<SeckillGoodsVO> detail(@PathVariable Long goodsId) {
        return Result.success(seckillService.getSeckillDetail(goodsId));
    }

    /**
     * 发布秒杀商品（通常由发布二手商品时或后台配置）
     */
    @PostMapping("/publish")
    @FastAuthorize(required = true)
    public Result<?> publish(@RequestBody SeckillPublishDTO req) {
        Goods goods = goodsService.getGoodsById(req.getGoodsId());
        if (goods == null) {
            return Result.error("二手商品不存在");
        }
        if (!goods.getSellerId().equals(UserContext.getUserId())) {
            return Result.error("只能将自己发布的二手商品转为秒杀抢购");
        }

        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setGoodsId(req.getGoodsId());
        seckillGoods.setSeckillPrice(req.getSeckillPrice());
        seckillGoods.setStock(req.getStock());
        seckillGoods.setStartTime(req.getStartTime());
        seckillGoods.setEndTime(req.getEndTime());
        seckillGoods.setCreateTime(LocalDateTime.now());
        seckillService.publishSeckill(seckillGoods);

        // 自动进行库存预热，方便测试
        seckillService.warmUpStock(req.getGoodsId());

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
