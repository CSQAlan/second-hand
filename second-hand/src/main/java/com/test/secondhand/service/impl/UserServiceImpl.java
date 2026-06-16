package com.test.secondhand.service.impl;

import com.test.secondhand.entity.User;
import com.test.secondhand.mapper.UserMapper;
import com.test.secondhand.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.test.secondhand.mapper.GoodsMapper goodsMapper;

    @Autowired
    private com.test.secondhand.mapper.OrderMapper orderMapper;

    @Autowired
    private com.test.secondhand.mapper.ReviewMapper reviewMapper;

    @Override
    public User getUserProfile(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public void updateUserProfile(Long userId, User updateData) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new com.test.secondhand.exception.BusinessException("用户不存在");
        }

        // 只更新允许修改的字段
        if (updateData.getNickname() != null) {
            existing.setNickname(updateData.getNickname());
        }
        if (updateData.getAvatar() != null) {
            existing.setAvatar(updateData.getAvatar());
        }
        if (updateData.getPhone() != null) {
            existing.setPhone(updateData.getPhone());
        }
        if (updateData.getEmail() != null) {
            existing.setEmail(updateData.getEmail());
        }
        if (updateData.getGender() != null) {
            existing.setGender(updateData.getGender());
        }
        if (updateData.getAddress() != null) {
            existing.setAddress(updateData.getAddress());
        }
        if (updateData.getBio() != null) {
            existing.setBio(updateData.getBio());
        }
        existing.setUpdateTime(LocalDateTime.now());

        userMapper.updateById(existing);
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 发布商品数
        Long publishCount = goodsMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.test.secondhand.entity.Goods>()
                        .eq(com.test.secondhand.entity.Goods::getSellerId, userId));
        stats.put("publishCount", publishCount);

        // 卖出数量（状态为已售的）
        Long sellCount = goodsMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.test.secondhand.entity.Goods>()
                        .eq(com.test.secondhand.entity.Goods::getSellerId, userId)
                        .eq(com.test.secondhand.entity.Goods::getStatus, 1));
        stats.put("sellCount", sellCount);

        // 买入数量
        Long buyCount = orderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.test.secondhand.entity.Order>()
                        .eq(com.test.secondhand.entity.Order::getBuyerId, userId));
        stats.put("buyCount", buyCount);

        // 收到的评价数
        Long reviewCount = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.test.secondhand.entity.Review>()
                        .eq(com.test.secondhand.entity.Review::getTargetUserId, userId));
        stats.put("reviewCount", reviewCount);

        return stats;
    }

    @Override
    public Map<String, Object> getUserPublicInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new com.test.secondhand.exception.BusinessException("用户不存在");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickname", user.getNickname());
        info.put("avatar", user.getAvatar());
        info.put("bio", user.getBio());
        info.put("createTime", user.getCreateTime());

        return info;
    }
}
