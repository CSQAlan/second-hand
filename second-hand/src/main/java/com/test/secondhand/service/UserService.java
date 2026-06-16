package com.test.secondhand.service;

import com.test.secondhand.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户个人信息
     */
    User getUserProfile(Long userId);

    /**
     * 更新用户个人信息
     */
    void updateUserProfile(Long userId, User updateData);

    /**
     * 获取用户统计信息
     */
    Map<String, Object> getUserStats(Long userId);

    /**
     * 获取用户公开信息（脱敏）
     */
    Map<String, Object> getUserPublicInfo(Long userId);
}
