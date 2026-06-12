package com.test.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.Order;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.security.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@FastAuthorize(required = true) // 此 Controller 所有接口均需要登录
public class OrderController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private com.test.secondhand.service.OrderService orderService;

    public static class OrderVO {
        private Long id;
        private String orderNo;
        private Long goodsId;
        private String goodsName;
        private String goodsImageUrl;
        private Long buyerId;
        private Long sellerId;
        private java.math.BigDecimal price;
        private Integer type; // 0-普通购买, 1-秒杀抢购, 2-拍卖成交
        private Integer status; // 0-待付款, 1-已付款, 2-已发货, 3-已收货, 4-交易成功, 5-已取消
        private String deliveryNo;
        private java.time.LocalDateTime payTime;
        private java.time.LocalDateTime shipTime;
        private java.time.LocalDateTime receiveTime;
        private java.time.LocalDateTime cancelTime;
        private String cancelReason;
        private java.time.LocalDateTime createTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public Long getGoodsId() {
            return goodsId;
        }

        public void setGoodsId(Long goodsId) {
            this.goodsId = goodsId;
        }

        public String getGoodsName() {
            return goodsName;
        }

        public void setGoodsName(String goodsName) {
            this.goodsName = goodsName;
        }

        public String getGoodsImageUrl() {
            return goodsImageUrl;
        }

        public void setGoodsImageUrl(String goodsImageUrl) {
            this.goodsImageUrl = goodsImageUrl;
        }

        public Long getBuyerId() {
            return buyerId;
        }

        public void setBuyerId(Long buyerId) {
            this.buyerId = buyerId;
        }

        public Long getSellerId() {
            return sellerId;
        }

        public void setSellerId(Long sellerId) {
            this.sellerId = sellerId;
        }

        public java.math.BigDecimal getPrice() {
            return price;
        }

        public void setPrice(java.math.BigDecimal price) {
            this.price = price;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getDeliveryNo() {
            return deliveryNo;
        }

        public void setDeliveryNo(String deliveryNo) {
            this.deliveryNo = deliveryNo;
        }

        public java.time.LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(java.time.LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public java.time.LocalDateTime getPayTime() {
            return payTime;
        }

        public void setPayTime(java.time.LocalDateTime payTime) {
            this.payTime = payTime;
        }

        public java.time.LocalDateTime getShipTime() {
            return shipTime;
        }

        public void setShipTime(java.time.LocalDateTime shipTime) {
            this.shipTime = shipTime;
        }

        public java.time.LocalDateTime getReceiveTime() {
            return receiveTime;
        }

        public void setReceiveTime(java.time.LocalDateTime receiveTime) {
            this.receiveTime = receiveTime;
        }

        public java.time.LocalDateTime getCancelTime() {
            return cancelTime;
        }

        public void setCancelTime(java.time.LocalDateTime cancelTime) {
            this.cancelTime = cancelTime;
        }

        public String getCancelReason() {
            return cancelReason;
        }

        public void setCancelReason(String cancelReason) {
            this.cancelReason = cancelReason;
        }
    }

    /**
     * 买家视角：获取我买到的所有订单
     */
    @GetMapping("/buyer")
    public Result<List<OrderVO>> getMyBoughtOrders() {
        Long buyerId = UserContext.getUserId();
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getCreateTime)
        );
        return Result.success(assembleVOList(orders));
    }

    /**
     * 卖家视角：获取我卖出的所有订单
     */
    @GetMapping("/seller")
    public Result<List<OrderVO>> getMySoldOrders() {
        Long sellerId = UserContext.getUserId();
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreateTime)
        );
        return Result.success(assembleVOList(orders));
    }

    /**
     * 商家发货
     */
    @PostMapping("/ship/{orderNo}")
    public Result<?> shipOrder(@PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String deliveryNo = body.get("deliveryNo");
        if (deliveryNo == null || deliveryNo.trim().isEmpty()) {
            return Result.error("快递单号或发货说明不能为空");
        }

        Long userId = UserContext.getUserId();
        orderService.shipOrder(orderNo, userId, deliveryNo);
        return Result.success();
    }

    /**
     * 买家确认收货
     */
    @PostMapping("/receive/{orderNo}")
    public Result<?> receiveOrder(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        orderService.receiveOrder(orderNo, userId);
        return Result.success();
    }

    /**
     * 买家付款
     */
    @PostMapping("/pay/{orderNo}")
    public Result<?> payOrder(@PathVariable String orderNo) {
        Long userId = UserContext.getUserId();
        orderService.payOrder(orderNo, userId);
        return Result.success("付款成功");
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderNo}")
    public Result<?> cancelOrder(@PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        Long userId = UserContext.getUserId();
        orderService.cancelOrder(orderNo, userId, reason);
        return Result.success("订单已取消");
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderNo}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }

        Long userId = UserContext.getUserId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error(403, "无权查看此订单");
        }

        Goods goods = goodsMapper.selectById(order.getGoodsId());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("order", order);
        result.put("goods", goods);

        return Result.success(result);
    }

    private List<OrderVO> assembleVOList(List<Order> orders) {
        List<OrderVO> voList = new ArrayList<>();
        for (Order o : orders) {
            Goods g = goodsMapper.selectById(o.getGoodsId());
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
