package com.test.secondhand.controller;

import com.test.secondhand.common.Result;
import com.test.secondhand.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/ask")
    public Result<String> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.error("提问内容不能为空");
        }
        String answer = chatbotService.getChatResponse(question);
        return Result.success(answer);
    }
}
