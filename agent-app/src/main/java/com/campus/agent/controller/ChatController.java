package com.campus.agent.controller;

import com.campus.agent.model.ChatResponse;
import com.campus.agent.service.CampusAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天控制器
 *
 * 用法：
 *   GET  /chat?message=南食堂在哪
 *   POST /chat  { "message": "南食堂在哪" }
 *   GET  /      首页
 */
@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final CampusAIService campusAIService;

    public ChatController(CampusAIService campusAIService) {
        this.campusAIService = campusAIService;
    }

    /**
     * 首页 — 直接重定向到前端页面
     */
    @GetMapping("/")
    public String index() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="refresh" content="0;url=/index.html">
                    <title>校园 AI 助手</title>
                </head>
                <body>
                    <p>正在跳转到 <a href="/index.html">校园 AI 助手</a>...</p>
                </body>
                </html>
                """;
    }

    /**
     * GET 聊天 — 简单测试用
     */
    @GetMapping("/chat")
    public ChatResponse chat(@RequestParam(value = "message", required = false) String message) {
        if (message == null || message.isBlank()) {
            return new ChatResponse("请告诉我你的问题！例如：/chat?message=南食堂在哪", null);
        }
        try {
            return campusAIService.askAI(message);
        } catch (Exception e) {
            log.error("AI 问答出错: {}", e.getMessage(), e);
            return new ChatResponse("小C遇到了一点问题：" + e.getMessage(), null);
        }
    }

    /**
     * POST 聊天 — 前端主调用的接口
     * 请求体：{ "message": "体育选修课选课时间" }
     * 返回：{ "answer": "...", "sources": [...] }
     */
    @PostMapping("/chat")
    public ChatResponse chatPost(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return new ChatResponse("请输入问题！", null);
        }
        try {
            return campusAIService.askAI(message);
        } catch (Exception e) {
            log.error("AI 问答出错: {}", e.getMessage(), e);
            return new ChatResponse("小C遇到了一点问题：" + e.getMessage(), null);
        }
    }
}
