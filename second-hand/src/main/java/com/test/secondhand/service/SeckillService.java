package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Order;
import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.mapper.SeckillGoodsMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class SeckillService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);
        log.info("[秒杀系统] 成功加载秒杀 Lua 脚本");
    }

    /**
     * 库存预热：将商品秒杀库存加载到 Redis
     */
    public void warmUpStock(Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId));
        if (seckillGoods == null) {
            throw new BusinessException("秒杀商品不存在");
        }

        String stockKey = "{seckill:" + goodsId + "}:stock";
        String userKey = "{seckill:" + goodsId + "}:users";

        // 将库存写入 Redis，并删除已秒杀用户集合，初始化缓存
        redisTemplate.opsForValue().set(stockKey, String.valueOf(seckillGoods.getStock()));
        redisTemplate.delete(userKey);

        log.info("[秒杀预热] 成功预热商品库存，商品ID: {}, 库存量: {}", goodsId, seckillGoods.getStock());
    }

    /**
     * 高并发秒杀抢购入口
     */
    public int executeSeckill(Long goodsId, Long userId) {
        String stockKey = "{seckill:" + goodsId + "}:stock";
        String userKey = "{seckill:" + goodsId + "}:users";

        // 执行 Lua 脚本，原子性地扣减库存并防重
        Long result = redisTemplate.execute(seckillScript, 
                Arrays.asList(stockKey, userKey), 
                String.valueOf(userId));

        if (result == null) {
            throw new BusinessException("服务器繁忙，请稍后再试");
        }

        int code = result.intValue();
        if (code == 1) {
            log.info("[秒杀成功] 用户: {}, 商品: {}。开始提交异步订单生成任务...", userId, goodsId);
            // 异步下单落库，使用自建的线程池 seckillExecutor
            createOrderAsync(goodsId, userId);
        }

        return code;
    }

    /**
     * 异步生成订单（数据库落库）
     */
    @Async("seckillExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void createOrderAsync(Long goodsId, Long userId) {
        try {
            // 1. 扣减数据库秒杀库存
            Long count = seckillGoodsMapper.selectCount(
                    new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId));
            if (count == null || count <= 0) {
                log.error("[异步下单异常] 数据库秒杀商品不存在，商品ID: {}", goodsId);
                return;
            }

            // 更新库存，乐观锁防止超卖（虽然 Redis Lua 已经过滤，但数据库层加一层校验更稳妥）
            SeckillGoods seckillGoods = seckillGoodsMapper.selectOne(
                    new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId));
            
            if (seckillGoods.getStock() <= 0) {
                log.error("[异步下单异常] 数据库库存不足，商品ID: {}", goodsId);
                return;
            }
            
            seckillGoods.setStock(seckillGoods.getStock() - 1);
            seckillGoodsMapper.updateById(seckillGoods);

            // 2. 获取商品卖家信息
            Goods goods = goodsMapper.selectById(goodsId);
            if (goods == null) {
                log.error("[异步下单异常] 原二手商品不存在，商品ID: {}", goodsId);
                return;
            }

            // 修改原商品状态为已售 (1)
            goods.setStatus(1);
            goodsMapper.updateById(goods);

            // 3. 写入订单表
            Order order = new Order();
            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
            order.setGoodsId(goodsId);
            order.setBuyerId(userId);
            order.setSellerId(goods.getSellerId());
            order.setPrice(seckillGoods.getSeckillPrice());
            order.setType(1); // 1-秒杀抢购
            order.setStatus(1); // 已付款（模拟秒杀成功即直接下单成功）
            order.setPayTime(LocalDateTime.now());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            log.info("[异步下单成功] 订单生成。订单号: {}, 买家ID: {}, 商品ID: {}", order.getOrderNo(), userId, goodsId);

        } catch (Exception e) {
            log.error("[异步下单失败] 用户ID: {}, 商品ID: {}", userId, goodsId, e);
            // 这里还可以抛出异常让 Spring 事务回滚，并考虑是否需要对 Redis 库存进行“回滚”补偿，
            // 通常工业界对非常少量的异步下单失败会计入差错日志，通过对账或重试处理。
            throw e; 
        }
    }
}
