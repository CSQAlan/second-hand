package com.test.secondhand.controller;

import com.test.secondhand.common.Result;
import com.test.secondhand.dto.LoginDTO;
import com.test.secondhand.dto.RegisterDTO;
import com.test.secondhand.service.AuthService;
import com.test.secondhand.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDTO request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.error("用户名和密码不能为空");
        }

        authService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()
                || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return Result.error("用户名和密码不能为空");
        }

        Map<String, Object> data = authService.login(request.getUsername(), request.getPassword());

        LoginVO vo = new LoginVO();
        vo.setToken((String) data.get("token"));
        vo.setUserId((Long) data.get("userId"));
        vo.setUsername((String) data.get("username"));
        vo.setNickname((String) data.get("nickname"));
        vo.setRole((String) data.get("role"));

        return Result.success(vo);
    }
}
