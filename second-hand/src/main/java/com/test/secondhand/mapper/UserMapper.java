package com.test.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
