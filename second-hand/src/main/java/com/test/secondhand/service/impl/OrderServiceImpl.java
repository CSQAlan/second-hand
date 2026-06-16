package com.test.secondhand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.Order;
import com.test.secondhand.exception.BusinessException;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.service.GoodsService;
import com.test.secondhand.service.OrderService;
import com.test.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsService goodsService;

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    @Override
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

    @Override
    public List<OrderVO> getBuyerOrders(Long buyerId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getCreateTime)
        );
        return assembleOrderVOList(orders);
    }

    @Override
    public List<OrderVO> getSellerOrders(Long sellerId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreateTime)
        );
        return assembleOrderVOList(orders);
    }

    @Override
    public Map<String, Object> getOrderDetail(String orderNo, Long userId) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }

        Goods goods = goodsMapper.selectById(order.getGoodsId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("goods", goods);
        return result;
    }

    /**
     * 批量组装订单 VO，解决 N+1 查询问题
     * 一次性加载所有关联商品，而非逐条查询
     */
    private List<OrderVO> assembleOrderVOList(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        // 一次性收集所有商品 ID，批量查询
        Set<Long> goodsIds = orders.stream()
                .map(Order::getGoodsId)
                .collect(Collectors.toSet());
        Map<Long, Goods> goodsMap = goodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));

        List<OrderVO> voList = new ArrayList<>();
        for (Order o : orders) {
            Goods g = goodsMap.get(o.getGoodsId());
            OrderVO vo = new OrderVO();
            vo.setId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setGoodsId(o.getGoodsId());
            vo.setGoodsName(g != null ? g.getName() : "未知商品");
            vo.setGoodsImageUrl(g != null ? g.getImageUrl() : "");
            vo.setBuyerId(o.getBuyerId());
            vo.setSellerId(o.getSellerId());
            vo.setPrice(o.getPrice());
            vo.setType(o.getType());
            vo.setStatus(o.getStatus());
            vo.setDeliveryNo(o.getDeliveryNo());
            vo.setPayTime(o.getPayTime());
            vo.setShipTime(o.getShipTime());
            vo.setReceiveTime(o.getReceiveTime());
            vo.setCancelTime(o.getCancelTime());
            vo.setCancelReason(o.getCancelReason());
            vo.setCreateTime(o.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }
}
