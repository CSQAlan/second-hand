package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.User;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取个人信息
     */
    @GetMapping("/profile")
    @FastAuthorize(required = true)
    public Result<User> getProfile() {
        Long userId = UserContext.getUserId();
        User user = userService.getUserProfile(userId);
        // 清除密码字段
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    @FastAuthorize(required = true)
    public Result<?> updateProfile(@RequestBody User updateData) {
        Long userId = UserContext.getUserId();
        userService.updateUserProfile(userId, updateData);
        return Result.success("更新成功");
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    @FastAuthorize(required = true)
    public Result<Map<String, Object>> getStats() {
        Long userId = UserContext.getUserId();
        return Result.success(userService.getUserStats(userId));
    }

    /**
     * 获取用户公开信息
     */
    @GetMapping("/{userId}/public")
    public Result<Map<String, Object>> getPublicInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserPublicInfo(userId));
    }
}
