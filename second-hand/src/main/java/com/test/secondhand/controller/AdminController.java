package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@FastAuthorize(required = true)
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        if (!"ROLE_ADMIN".equals(UserContext.getRole())) {
            return Result.error(403, "无权访问管理员面板");
        }
        return Result.success(adminService.getDashboardStats());
    }

    @GetMapping("/operations")
    public Result<List<Map<String, Object>>> getPlatformOperations() {
        if (!"ROLE_ADMIN".equals(UserContext.getRole())) {
            return Result.error(403, "无权访问平台操作日志");
        }
        try {
            return Result.success(adminService.getPlatformOperations());
        } catch (Exception e) {
            return Result.error("获取操作日志异常: " + e.getMessage());
        }
    }
}
