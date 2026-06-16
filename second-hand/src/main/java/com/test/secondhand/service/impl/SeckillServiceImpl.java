package com.test.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Order;
import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.mapper.SeckillGoodsMapper;
import com.test.secondhand.service.SeckillService;
import com.test.secondhand.vo.SeckillGoodsVO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    private DefaultRedisScript<Long> seckillScript;

    @Override
    public List<SeckillGoodsVO> getAllSeckillGoodsList() {
        List<SeckillGoods> list = seckillGoodsMapper.selectList(new LambdaQueryWrapper<>());
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量加载关联商品，避免 N+1
        Set<Long> goodsIds = list.stream().map(SeckillGoods::getGoodsId).collect(Collectors.toSet());
        Map<Long, Goods> goodsMap = goodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        LocalDateTime now = LocalDateTime.now();
        List<SeckillGoodsVO> voList = new ArrayList<>();
        for (SeckillGoods sg : list) {
            Goods goods = goodsMap.get(sg.getGoodsId());
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
        return voList;
    }

    @Override
    public SeckillGoodsVO getSeckillDetail(Long goodsId) {
        SeckillGoods sg = seckillGoodsMapper.selectOne(
                new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getGoodsId, goodsId));
        if (sg == null) {
            throw new BusinessException("秒杀商品不存在");
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
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
        return vo;
    }

    @Override
    public void publishSeckill(SeckillGoods seckillGoods) {
        seckillGoodsMapper.insert(seckillGoods);
    }

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        seckillScript.setResultType(Long.class);
        log.info("[秒杀系统] 成功加载秒杀 Lua 脚本");
    }

    @Override
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

    @Override
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

    @Override
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
            throw e;
        }
    }
}
