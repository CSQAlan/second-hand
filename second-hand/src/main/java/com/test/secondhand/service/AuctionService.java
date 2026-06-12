package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.AuctionGoods;
import com.test.secondhand.entity.AuctionRecord;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.Order;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.AuctionGoodsMapper;
import com.test.secondhand.mapper.AuctionRecordMapper;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuctionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuctionService.class);

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AuctionGoodsMapper auctionGoodsMapper;

    @Autowired
    private AuctionRecordMapper auctionRecordMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String AUCTION_PRICE_CACHE_PREFIX = "auction:price:";

    /**
     * 用户对拍卖商品进行出价
     */
    @Transactional(rollbackFor = Exception.class)
    public void placeBid(Long auctionGoodsId, Long userId, BigDecimal bidPrice) {
        String lockKey = "lock:auction:" + auctionGoodsId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，最多等待3秒，获取到后持锁5秒
            if (lock.tryLock(3, 5, TimeUnit.SECONDS)) {
                try {
                    // 1. 查询拍卖商品状态
                    AuctionGoods auctionGoods = auctionGoodsMapper.selectById(auctionGoodsId);
                    if (auctionGoods == null) {
                        throw new BusinessException("拍卖商品不存在");
                    }
                    if (auctionGoods.getStatus() == 1 || LocalDateTime.now().isAfter(auctionGoods.getEndTime())) {
                        throw new BusinessException("该拍卖活动已结束");
                    }
                    if (LocalDateTime.now().isBefore(auctionGoods.getStartTime())) {
                        throw new BusinessException("该拍卖活动尚未开始");
                    }

                    // 2. 校验出价金额
                    BigDecimal minRequiredPrice = auctionGoods.getCurrentPrice().compareTo(BigDecimal.ZERO) == 0 
                            ? auctionGoods.getStartPrice() 
                            : auctionGoods.getCurrentPrice().add(auctionGoods.getMinIncrement());

                    if (bidPrice.compareTo(minRequiredPrice) < 0) {
                        throw new BusinessException("出价不能低于最低加价额度，当前所需最低出价为: " + minRequiredPrice);
                    }

                    // 3. 更新出价信息
                    auctionGoods.setCurrentPrice(bidPrice);
                    auctionGoods.setHighestBidderId(userId);
                    auctionGoodsMapper.updateById(auctionGoods);

                    // 4. 记录出价记录
                    AuctionRecord record = new AuctionRecord();
                    record.setAuctionGoodsId(auctionGoodsId);
                    record.setBidderId(userId);
                    record.setBidPrice(bidPrice);
                    record.setBidTime(LocalDateTime.now());
                    auctionRecordMapper.insert(record);

                    // 5. 缓存出价价格到 Redis，提供高并发实时获取最高出价的支撑
                    redisTemplate.opsForValue().set(
                            AUCTION_PRICE_CACHE_PREFIX + auctionGoodsId, 
                            bidPrice.toString(), 
                            1, TimeUnit.DAYS
                    );

                    log.info("[拍卖出价成功] 商品ID: {}, 用户ID: {}, 出价金额: {}", auctionGoodsId, userId, bidPrice);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new BusinessException("出价人数较多，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("服务器拥挤，出价失败");
        }
    }

    /**
     * 定时任务：每 10 秒扫描一次已结束的拍卖，进行自动结标及生成订单
     */
    @Scheduled(cron = "0/10 * * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void closeEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查找所有已过期且尚未结标的商品
        List<AuctionGoods> endedAuctions = auctionGoodsMapper.selectList(
                new LambdaQueryWrapper<AuctionGoods>()
                        .eq(AuctionGoods::getStatus, 0)
                        .le(AuctionGoods::getEndTime, now)
        );

        if (endedAuctions.isEmpty()) {
            return;
        }

        log.info("[拍卖结算] 扫描到 {} 个到期的拍卖商品，开始执行自动结标...", endedAuctions.size());

        for (AuctionGoods auction : endedAuctions) {
            // 获取分布式锁，防止集群节点重复结算
            String lockKey = "lock:auction:settle:" + auction.getId();
            RLock lock = redissonClient.getLock(lockKey);
            
            try {
                if (lock.tryLock(0, 5, TimeUnit.SECONDS)) {
                    try {
                        // 双重校验状态
                        AuctionGoods currentAuction = auctionGoodsMapper.selectById(auction.getId());
                        if (currentAuction == null || currentAuction.getStatus() == 1) {
                            continue;
                        }

                        // 1. 修改拍卖状态为已结束 (1)
                        currentAuction.setStatus(1);
                        auctionGoodsMapper.updateById(currentAuction);

                        Goods goods = goodsMapper.selectById(currentAuction.getGoodsId());
                        if (goods == null) {
                            continue;
                        }

                        if (currentAuction.getHighestBidderId() != null) {
                            // 2. 有人出价：修改商品状态为已售 (1)
                            goods.setStatus(1);
                            goodsMapper.updateById(goods);

                            // 3. 自动生成订单
                            Order order = new Order();
                            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
                            order.setGoodsId(currentAuction.getGoodsId());
                            order.setBuyerId(currentAuction.getHighestBidderId());
                            order.setSellerId(goods.getSellerId());
                            order.setPrice(currentAuction.getCurrentPrice());
                            order.setType(2); // 2-拍卖成交
                            order.setStatus(0); // 待付款（成交后买家需进行付款）
                            order.setCreateTime(LocalDateTime.now());
                            order.setUpdateTime(LocalDateTime.now());
                            orderMapper.insert(order);

                            log.info("[拍卖结标成功] 竞拍成功！商品ID: {}, 竞得人ID: {}, 成交价: {}, 订单号: {}", 
                                    currentAuction.getGoodsId(), currentAuction.getHighestBidderId(), currentAuction.getCurrentPrice(), order.getOrderNo());
                        } else {
                            // 4. 无人出价：流拍，修改原二手商品状态为已下架 (2)
                            goods.setStatus(2);
                            goodsMapper.updateById(goods);
                            log.info("[拍卖流拍] 商品ID: {}, 无人出价，自动下架", currentAuction.getGoodsId());
                        }

                        // 清除缓存中的出价信息
                        redisTemplate.delete(AUCTION_PRICE_CACHE_PREFIX + auction.getId());

                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Exception e) {
                log.error("[拍卖自动结算失败] 拍卖ID: {}", auction.getId(), e);
            }
        }
    }
}
