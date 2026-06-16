package com.test.secondhand.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.secondhand.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
