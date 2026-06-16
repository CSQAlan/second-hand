package com.test.secondhand.vo;

import lombok.Data;

/**
 * 管理后台统计视图对象
 */
@Data
public class AdminStatsVO {

    private Long totalUsers;
    private Long totalGoods;
    private Long totalOrders;
    private Long todayNewUsers;
    private Long todayNewGoods;
    private Long todayNewOrders;
}
