package com.test.secondhand.controller;

import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.User;
import com.test.secondhand.security.UserContext;
import com.test.secondhand.service.UserService;
import com.test.secondhand.vo.UserPublicVO;
import com.test.secondhand.vo.UserStatsVO;
import com.test.secondhand.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Result<UserVO> getProfile() {
        Long userId = UserContext.getUserId();
        User user = userService.getUserProfile(userId);
        UserVO vo = UserVO.from(user);
        return Result.success(vo);
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
    public Result<UserStatsVO> getStats() {
        Long userId = UserContext.getUserId();
        java.util.Map<String, Object> stats = userService.getUserStats(userId);

        UserStatsVO vo = new UserStatsVO();
        vo.setPublishCount((Long) stats.get("publishCount"));
        vo.setSellCount((Long) stats.get("sellCount"));
        vo.setBuyCount((Long) stats.get("buyCount"));
        vo.setReviewCount((Long) stats.get("reviewCount"));

        return Result.success(vo);
    }

    /**
     * 获取用户公开信息
     */
    @GetMapping("/{userId}/public")
    public Result<UserPublicVO> getPublicInfo(@PathVariable Long userId) {
        java.util.Map<String, Object> info = userService.getUserPublicInfo(userId);

        UserPublicVO vo = new UserPublicVO();
        vo.setId((Long) info.get("id"));
        vo.setNickname((String) info.get("nickname"));
        vo.setAvatar((String) info.get("avatar"));
        vo.setBio((String) info.get("bio"));
        vo.setCreateTime((java.time.LocalDateTime) info.get("createTime"));

        return Result.success(vo);
    }
}
