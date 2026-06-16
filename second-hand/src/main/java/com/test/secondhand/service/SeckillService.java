package com.test.secondhand.service;

import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.vo.SeckillGoodsVO;

import java.util.List;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 获取所有秒杀商品列表（含商品信息）
     */
    List<SeckillGoodsVO> getAllSeckillGoodsList();

    /**
     * 获取单个秒杀商品详情（含商品信息）
     */
    SeckillGoodsVO getSeckillDetail(Long goodsId);

    /**
     * 发布秒杀商品
     */
    void publishSeckill(SeckillGoods seckillGoods);

    /**
     * 库存预热：将商品秒杀库存加载到 Redis
     */
    void warmUpStock(Long goodsId);

    /**
     * 高并发秒杀抢购入口
     */
    int executeSeckill(Long goodsId, Long userId);

    /**
     * 异步生成订单（数据库落库）
     */
    void createOrderAsync(Long goodsId, Long userId);
}
