package com.campus.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 校园 AI 助手 — 核心服务层
 *
 * 这里封装了与大模型（DeepSeek）的对话逻辑。
 * 关键概念：
 *   - System Message（系统消息）：告诉 AI 它的身份和知识库，每次对话都带着
 *   - User Message（用户消息）：你当前问的具体问题
 *
 * Phase 1 实现：把校园知识库放在 system prompt 里，
 * AI 会基于这些知识来回答问题。这是最简单的"伪 RAG"方式。
 * Phase 2 我们会替换成真正的向量检索（Vector RAG）。
 */
@Service
public class CampusAIService {

    private final ChatClient chatClient;

    /**
     * 构造器：注入 ChatClient Bean 和校园知识库文件
     *
     * @param chatClientBuilder Spring AI 自动创建的 ChatClient.Builder
     * @param campusInfoResource 从 classpath:knowledge/campus-info.md 加载的知识库
     */
    public CampusAIService(
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:knowledge/campus-info.md") Resource campusInfoResource
    ) throws IOException {

        // 1. 读取校园知识库文件内容
        String campusKnowledge = StreamUtils.copyToString(
                campusInfoResource.getInputStream(),
                StandardCharsets.UTF_8
        );

        // 2. 构建 System Prompt（系统提示词）
        //    这是 AI 的"角色设定" + "知识库"，每次对话都会带上
        String systemPrompt = """
                你是一位热情、专业的校园 AI 助手，名叫"小C"。
                你服务于 XX 大学的学生和教职工。
                
                ===== 校园知识库 =====
                %s
                ===== 知识库结束 =====
                
                回答要求：
                1. 基于上面的知识库回答问题，如果知识库中没有相关信息，直接说"抱歉，我目前还不知道这个信息"
                2. 回答要简洁清晰，用中文
                3. 对于地点类问题，给出具体位置描述
                4. 对于流程类问题，给出步骤说明
                """.formatted(campusKnowledge);

        // 3. 构建 ChatClient（聊天客户端）
        //    defaultSystem() 设置默认的系统消息
        //    这样每次调用 chatClient.prompt().user(...) 时，都会自动带上 system prompt
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 问 AI 一个问题
     *
     * @param question 用户的问题
     * @return AI 的回答文本
     */
    public String askAI(String question) {
        return chatClient.prompt()
                .user(question)      // 用户当前问题
                .call()              // 调用 DeepSeek API
                .content();          // 获取 AI 的回复文本
    }
}
