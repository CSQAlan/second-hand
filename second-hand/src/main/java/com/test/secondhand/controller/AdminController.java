package com.test.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.Goods;
import com.test.secondhand.entity.Order;
import com.test.secondhand.entity.SeckillGoods;
import com.test.secondhand.mapper.GoodsMapper;
import com.test.secondhand.mapper.OrderMapper;
import com.test.secondhand.mapper.SeckillGoodsMapper;
import com.test.secondhand.mapper.UserMapper;
import com.test.secondhand.security.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@FastAuthorize(required = true)
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        // 1. 严格控制权限：非管理员拒绝访问
        if (!"ROLE_ADMIN".equals(UserContext.getRole())) {
            return Result.error(403, "无权访问管理员面板");
        }

        Map<String, Object> stats = new HashMap<>();

        // 2. 核心指标卡片数据
        Long totalUsers = userMapper.selectCount(new LambdaQueryWrapper<>());
        stats.put("totalUsers", totalUsers);

        List<Order> validOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().in(Order::getStatus, 1, 2, 3)
        );
        BigDecimal totalSales = validOrders.stream()
                .map(Order::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalSales", totalSales);

        Long activeGoodsCount = goodsMapper.selectCount(
                new LambdaQueryWrapper<Goods>().eq(Goods::getStatus, 0)
        );
        stats.put("activeGoods", activeGoodsCount);

        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(new LambdaQueryWrapper<>());
        int totalSeckillStock = seckillGoodsList.stream()
                .mapToInt(SeckillGoods::getStock)
                .sum();
        stats.put("seckillStock", totalSeckillStock);

        // 3. 折线图数据：最近7天每日成交额与订单量
        List<String> dates = new ArrayList<>();
        List<BigDecimal> dailyAmounts = new ArrayList<>();
        List<Integer> dailyOrderCounts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));

            BigDecimal dailySum = BigDecimal.ZERO;
            int orderCount = 0;
            for (Order o : validOrders) {
                if (o.getCreateTime().toLocalDate().equals(date)) {
                    dailySum = dailySum.add(o.getPrice());
                    orderCount++;
                }
            }
            dailyAmounts.add(dailySum);
            dailyOrderCounts.add(orderCount);
        }
        
        Map<String, Object> lineChart = new HashMap<>();
        lineChart.put("categories", dates);
        lineChart.put("amounts", dailyAmounts);
        lineChart.put("orderCounts", dailyOrderCounts);
        stats.put("lineChart", lineChart);

        // 4. 饼图数据：按商品名称关键字，统计商品分类占比
        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<>());
        int digitalCount = 0;
        int bookCount = 0;
        int clothingCount = 0;
        int beautyCount = 0;
        int otherCount = 0;

        for (Goods g : goodsList) {
            String name = g.getName();
            if (name == null) continue;
            if (name.contains("手机") || name.contains("电脑") || name.contains("机") || name.contains("iPad") || name.contains("耳机")) {
                digitalCount++;
            } else if (name.contains("书") || name.contains("教材") || name.contains("简") || name.contains("阅读")) {
                bookCount++;
            } else if (name.contains("衣") || name.contains("鞋") || name.contains("裤") || name.contains("外套")) {
                clothingCount++;
            } else if (name.contains("妆") || name.contains("霜") || name.contains("面") || name.contains("洁")) {
                beautyCount++;
            } else {
                otherCount++;
            }
        }

        List<Map<String, Object>> pieChart = new ArrayList<>();
        pieChart.add(createPieItem("数码电子", digitalCount));
        pieChart.add(createPieItem("二手图书", bookCount));
        pieChart.add(createPieItem("服装鞋帽", clothingCount));
        pieChart.add(createPieItem("美妆日化", beautyCount));
        pieChart.add(createPieItem("其它闲置", otherCount));
        stats.put("pieChart", pieChart);

        // 5. 柱状图数据：订单分类统计（普通、秒杀、拍卖）
        int normalOrders = 0;
        int seckillOrders = 0;
        int auctionOrders = 0;
        for (Order o : validOrders) {
            if (o.getType() == 0) normalOrders++;
            else if (o.getType() == 1) seckillOrders++;
            else if (o.getType() == 2) auctionOrders++;
        }

        Map<String, Object> barChart = new HashMap<>();
        barChart.put("types", Arrays.asList("普通订单", "秒杀订单", "拍卖订单"));
        barChart.put("counts", Arrays.asList(normalOrders, seckillOrders, auctionOrders));
        stats.put("barChart", barChart);

        return Result.success(stats);
    }

    private Map<String, Object> createPieItem(String name, int value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }
}
