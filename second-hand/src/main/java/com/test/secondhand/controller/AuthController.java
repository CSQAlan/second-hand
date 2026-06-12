package com.test.secondhand.controller;

import com.test.secondhand.common.Result;
import com.test.secondhand.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String nickname = request.get("nickname");
        
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.error("用户名和密码不能为空");
        }
        
        authService.register(username, password, nickname);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Result.error("用户名和密码不能为空");
        }
        
        Map<String, Object> data = authService.login(username, password);
        return Result.success(data);
    }
}
