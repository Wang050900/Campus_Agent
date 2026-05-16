package com.campus.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import io.micrometer.observation.ObservationRegistry;

/**
 * DeepSeek 模型配置
 */
@Configuration
public class DeepSeekConfig {

    /**
     * 创建 DeepSeek API 客户端
     */
    @Bean
    public DeepSeekApi deepSeekApi(
            @Value("${spring.ai.deepseek.api-key}") String apiKey
    ) {
        return DeepSeekApi.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com")
                .build();
    }

    /**
     * 创建 DeepSeek 对话选项（模型参数）
     */
    @Bean
    public DeepSeekChatOptions deepSeekChatOptions(
            @Value("${spring.ai.deepseek.chat.options.model}") String model,
            @Value("${spring.ai.deepseek.chat.options.temperature}") Double temperature,
            @Value("${spring.ai.deepseek.chat.options.max-tokens}") Integer maxTokens
    ) {
        return DeepSeekChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    /**
     * ToolCallingManager Bean —— 管理 AI 工具调用
     *
     * 多个组件需要同一个 ToolCallingManager 实例：
     *   - DeepSeekChatModel：底层模型需要它来处理工具调用
     *   - ToolCallAdvisor：高级 advisor 也需要它
     *   所以提取为独立的 @Bean，避免重复创建。
     */
    @Bean
    public ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder().build();
    }

    /**
     * 创建 DeepSeek ChatModel（核心 Bean）
     *
     * ChatModel 是 Spring AI 中最核心的接口，代表一个大语言模型。
     * DeepSeekChatModel 就是 ChatModel 的 DeepSeek 实现。
     */
    @Bean
    public DeepSeekChatModel deepSeekChatModel(
            DeepSeekApi deepSeekApi,
            DeepSeekChatOptions deepSeekChatOptions,
            ToolCallingManager toolCallingManager
    ) {
        return DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(deepSeekChatOptions)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.create())
                .build();
    }

    /**
     * 创建 ChatClient.Builder（用于基础问答）
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel);
    }

    /**
     * 创建另一个 ChatClient.Builder（用于 RAG 问答）
     * 
     * 为什么要两个 Builder？
     * 因为基础问答和 RAG 问答的 System Prompt 不同，
     * 我们用不同的 Builder + defaultSystem() 来区分，
     * 避免互相覆盖。
     */
    @Bean
    public ChatClient.Builder ragChatClientBuilder(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel);
    }
}
