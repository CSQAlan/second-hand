package com.test.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.Order;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsService goodsService;

    /**
     * 付款
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("当前订单状态不允许付款");
        }

        order.setStatus(1); // 已付款
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("[订单付款成功] 订单号: {}", orderNo);
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId, String reason) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 只有买家可以取消待付款的订单
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        // 只有待付款状态可以取消
        if (order.getStatus() != 0) {
            throw new BusinessException("当前订单状态不允许取消");
        }

        order.setStatus(5); // 已取消
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        log.info("[订单取消成功] 订单号: {}, 原因: {}", orderNo, reason);
    }

    /**
     * 发货
     */
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(String orderNo, Long userId, String deliveryNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 1) {
            throw new BusinessException("只有已付款的订单才能发货");
        }

        order.setStatus(2); // 已发货
        order.setDeliveryNo(deliveryNo);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("[订单发货成功] 订单号: {}, 物流单号: {}", orderNo, deliveryNo);
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(String orderNo, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不允许确认收货");
        }

        order.setStatus(3); // 已收货
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("[确认收货成功] 订单号: {}", orderNo);
    }

    /**
     * 完成交易（评价后自动调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != 3) {
            throw new BusinessException("订单状态不正确");
        }

        order.setStatus(4); // 交易成功
        orderMapper.updateById(order);

        log.info("[交易完成] 订单号: {}", orderNo);
    }

    /**
     * 根据订单号获取订单
     */
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    /**
     * 创建订单并更新商品状态（包含事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long goodsId, Long buyerId) {
        Goods goods = goodsService.getGoodsById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        if (goods.getStatus() != 0) {
            throw new BusinessException("商品已被售出或下架");
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new BusinessException("不能购买自己发布的商品");
        }

        // 1. 更新商品状态为已售 (1)
        goods.setStatus(1);
        goodsService.updateGoods(goods);

        // 2. 创建订单并保存
        Order order = new Order();
        order.setOrderNo(java.util.UUID.randomUUID().toString().replace("-", ""));
        order.setGoodsId(goodsId);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        order.setPrice(goods.getPrice());
        order.setType(0); // 0-普通购买
        order.setStatus(1); // 1-已付款
        order.setPayTime(LocalDateTime.now()); // 补全 payTime
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        log.info("[创建订单成功] 订单号: {}, 买家: {}, 商品: {}", order.getOrderNo(), buyerId, goodsId);
        return order;
    }
}
