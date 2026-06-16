package com.test.secondhand.service;

import com.test.secondhand.entity.Order;
import com.test.secondhand.vo.OrderVO;

import java.util.List;
import java.util.Map;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 获取买家的所有订单（含商品信息）
     */
    List<OrderVO> getBuyerOrders(Long buyerId);

    /**
     * 获取卖家的所有订单（含商品信息）
     */
    List<OrderVO> getSellerOrders(Long sellerId);

    /**
     * 获取订单详情（含商品信息，带权限校验）
     */
    Map<String, Object> getOrderDetail(String orderNo, Long userId);

    /**
     * 付款
     */
    void payOrder(String orderNo, Long userId);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo, Long userId, String reason);

    /**
     * 发货
     */
    void shipOrder(String orderNo, Long userId, String deliveryNo);

    /**
     * 确认收货
     */
    void receiveOrder(String orderNo, Long userId);

    /**
     * 完成交易（评价后自动调用）
     */
    void completeOrder(String orderNo);

    /**
     * 根据订单号获取订单
     */
    Order getOrderByOrderNo(String orderNo);

    /**
     * 创建订单并更新商品状态
     */
    Order createOrder(Long goodsId, Long buyerId);
}
