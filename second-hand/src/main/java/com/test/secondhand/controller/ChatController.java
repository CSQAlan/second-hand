package com.test.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.secondhand.annotation.FastAuthorize;
import com.test.secondhand.common.Result;
import com.test.secondhand.entity.ChatMessage;
import com.test.secondhand.entity.User;
import com.test.secondhand.mapper.ChatMessageMapper;
import com.test.secondhand.mapper.UserMapper;
import com.test.secondhand.security.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@FastAuthorize(required = true) // 所有聊天相关接口必须登录
public class ChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取与特定用户的聊天历史记录
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam Long receiverId) {
        Long myId = UserContext.getUserId();
        
        List<ChatMessage> list = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .and(w -> w.eq(ChatMessage::getSenderId, myId).eq(ChatMessage::getReceiverId, receiverId))
                        .or(w -> w.eq(ChatMessage::getSenderId, receiverId).eq(ChatMessage::getReceiverId, myId))
                        .orderByAsc(ChatMessage::getCreateTime)
        );
        
        return Result.success(list);
    }

    /**
     * 发送聊天消息
     */
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestBody ChatMessage message) {
        if (message.getReceiverId() == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            return Result.error("接收者或消息内容不能为空");
        }

        Long myId = UserContext.getUserId();
        message.setSenderId(myId);
        message.setIsRead(0); // 默认为未读
        message.setCreateTime(LocalDateTime.now());
        
        chatMessageMapper.insert(message);
        return Result.success(message);
    }

    /**
     * 获取最近聊天联系人列表
     */
    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> getContacts() {
        Long myId = UserContext.getUserId();
        
        // 查询与当前用户相关的所有聊天消息，并按时间倒序排列
        List<ChatMessage> myMsgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSenderId, myId)
                        .or()
                        .eq(ChatMessage::getReceiverId, myId)
                        .orderByDesc(ChatMessage::getCreateTime)
        );

        // 去重获取每个联系人的最新一条聊天记录
        Map<Long, ChatMessage> latestMsgMap = new LinkedHashMap<>();
        for (ChatMessage msg : myMsgs) {
            Long otherId = msg.getSenderId().equals(myId) ? msg.getReceiverId() : msg.getSenderId();
            if (!latestMsgMap.containsKey(otherId)) {
                latestMsgMap.put(otherId, msg);
            }
        }

        // 装配联系人详细信息与未读消息数
        List<Map<String, Object>> contactList = new ArrayList<>();
        for (Map.Entry<Long, ChatMessage> entry : latestMsgMap.entrySet()) {
            Long contactId = entry.getKey();
            ChatMessage latestMsg = entry.getValue();

            User user = userMapper.selectById(contactId);
            if (user == null) continue;

            // 统计来自该联系人的未读消息数
            Long unreadCount = chatMessageMapper.selectCount(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSenderId, contactId)
                            .eq(ChatMessage::getReceiverId, myId)
                            .eq(ChatMessage::getIsRead, 0)
            );

            Map<String, Object> contact = new HashMap<>();
            contact.put("id", user.getId());
            contact.put("username", user.getUsername());
            contact.put("nickname", user.getNickname());
            contact.put("avatar", user.getAvatar());
            contact.put("latestMessage", latestMsg.getContent());
            contact.put("latestTime", latestMsg.getCreateTime());
            contact.put("unreadCount", unreadCount);

            contactList.add(contact);
        }

        return Result.success(contactList);
    }

    /**
     * 标记来自特定联系人的消息为已读
     */
    @PostMapping("/read")
    public Result<?> markAsRead(@RequestParam Long senderId) {
        Long myId = UserContext.getUserId();

        ChatMessage update = new ChatMessage();
        update.setIsRead(1);

        chatMessageMapper.update(update,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSenderId, senderId)
                        .eq(ChatMessage::getReceiverId, myId)
                        .eq(ChatMessage::getIsRead, 0)
        );

        return Result.success();
    }
}
