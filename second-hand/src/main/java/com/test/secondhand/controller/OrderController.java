package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.OrderService;
import com.test.secondhand.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@FastAuthorize(required = true) // 此 Controller 所有接口均需要登录
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 买家视角：获取我买到的所有订单
     */
    @GetMapping("/buyer")
    public Result<List<OrderVO>> getMyBoughtOrders() {
        Long buyerId = UserContext.getUserId();
        return Result.success(orderService.getBuyerOrders(buyerId));
    }

    /**
     * 卖家视角：获取我卖出的所有订单
     */
    @GetMapping("/seller")
    public Result<List<OrderVO>> getMySoldOrders() {
        Long sellerId = UserContext.getUserId();
        return Result.success(orderService.getSellerOrders(sellerId));
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
        Long userId = UserContext.getUserId();
        return Result.success(orderService.getOrderDetail(orderNo, userId));
    }
}
