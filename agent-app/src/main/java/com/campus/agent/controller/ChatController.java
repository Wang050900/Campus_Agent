package com.campus.agent.controller;

import com.campus.agent.service.CampusAIService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天控制器 — 提供 AI 问答的 REST API 接口
 *
 * 这个 Controller 负责接收用户的 HTTP 请求，调用 AI Service 处理，
 * 然后把结果返回给前端（浏览器、小程序等）。
 *
 * 你现在可以直接用浏览器或 curl 来测试。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final CampusAIService campusAIService;

    /**
     * 构造器注入 — Spring 会自动把 CampusAIService 传进来
     */
    public ChatController(CampusAIService campusAIService) {
        this.campusAIService = campusAIService;
    }

    /**
     * GET 请求：http://localhost:8080/chat?message=南食堂在哪
     *
     * @param message 用户输入的问题
     * @return AI 的回答
     */
    @GetMapping
    public String chat(@RequestParam("message") String message) {
        return campusAIService.askAI(message);
    }

    /**
     * POST 请求：更正式的 JSON 格式
     * 请求体：{ "message": "南食堂在哪" }
     *
     * @param request 包含 message 字段的 JSON
     * @return AI 的回答
     */
    @PostMapping
    public String chatPost(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return "请输入问题！";
        }
        return campusAIService.askAI(message);
    }
}
