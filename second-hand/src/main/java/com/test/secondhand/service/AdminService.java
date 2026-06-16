package com.test.secondhand.service;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 获取仪表盘统计数据（核心指标 + 图表数据）
     */
    Map<String, Object> getDashboardStats();

    /**
     * 获取平台操作日志（最近交易流水）
     */
    List<Map<String, Object>> getPlatformOperations();
}
