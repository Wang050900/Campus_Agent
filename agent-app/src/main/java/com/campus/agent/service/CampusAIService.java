package com.campus.agent.service;

import com.campus.agent.model.ChatResponse;
import com.campus.agent.model.ChatResponse.SourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 校园 AI 助手 — 核心服务层
 *
 * === 完整的 RAG 流程 ===
 *
 * 用户问："体育选修课选课时间"
 *    ↓
 *  1. R（Retrieval）RagService.search() 关键词匹配找到相关文章
 *    ↓  找到《体育选修课选课通知》
 *  2. A（Augmented）RagService.buildContext() 构建上下文
 *    ↓  把文章拼进 Prompt
 *  3. G（Generation）DeepSeek 基于文章内容生成回答
 *    ↓
 *  4. 返回回答 + 来源链接
 */
@Service
public class CampusAIService {

    private static final Logger log = LoggerFactory.getLogger(CampusAIService.class);

    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final RagService ragService;

    public CampusAIService(
            @Qualifier("chatClientBuilder") ChatClient.Builder chatClientBuilder,
            @Qualifier("ragChatClientBuilder") ChatClient.Builder ragChatClientBuilder,
            RagService ragService,
            @Value("classpath:knowledge/campus-info.md") Resource campusInfoResource
    ) throws IOException {

        this.ragService = ragService;

        // ---------- ChatClient 1：基础问答（带校园知识库） ----------
        String campusKnowledge = StreamUtils.copyToString(
                campusInfoResource.getInputStream(), StandardCharsets.UTF_8
        );

        String basicSystemPrompt = """
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

        this.chatClient = chatClientBuilder
                .defaultSystem(basicSystemPrompt)
                .build();

        // ---------- ChatClient 2：RAG 问答 ----------
        String ragSystemPrompt = """
                你是一位严谨的校园信息助手"小C"。
                
                你的工作流程：
                1. 用户问了一个问题
                2. 下面会提供"参考资料"，这些是从校园网抓取的真实文章
                3. 请**基于参考资料**回答用户的问题
                4. 回答时要简洁，先给一个 100 字以内的概括
                5. 概括之后，列出详细信息
                6. 最后标注"具体文件："并列出参考文章的标题和 URL
                
                重要规则：
                - 如果参考资料与问题无关，使用你自己的知识回答
                - 不要编造参考资料中没有的信息
                - 综合多条参考资料的信息来回答
                """;

        this.ragChatClient = ragChatClientBuilder
                .defaultSystem(ragSystemPrompt)
                .build();
    }

    /**
     * 问 AI 一个问题
     *
     * 自动判断：用 RAG 还是基础问答
     */
    public ChatResponse askAI(String question) {
        // 1. 先尝试 RAG：搜索相关文章
        List<SourceInfo> sources = ragService.search(question);

        if (!sources.isEmpty()) {
            // 有相关文章 → 用 RAG 回答
            String context = ragService.buildContext(question);
            log.info("📚 RAG 已找到相关文章，正在让 DeepSeek 基于文章回答...");

            String answer = ragChatClient.prompt()
                    .user(u -> u.text("""
                            用户问题：{question}
                            
                            参考资料：
                            {context}
                            
                            请基于以上参考资料回答用户问题。
                            """)
                            .param("question", question)
                            .param("context", context))
                    .call()
                    .content();

            return new ChatResponse(answer, sources);
        }

        // 2. 没有相关文章 → 用基础知识库回答
        log.info("📖 未找到相关文章，使用基础知识库回答");
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();

        return new ChatResponse(answer, null);
    }
}
