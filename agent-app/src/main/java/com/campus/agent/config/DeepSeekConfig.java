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
 *
 * 为什么需要手动配置？
 * spring-ai-deepseek 模块（1.1.6）提供了 DeepSeekChatModel，但没有自动配置（AutoConfiguration）。
 * 所以我们需要手动创建 DeepSeekApi → DeepSeekChatModel → ChatClient.Builder 的 Bean 链条。
 *
 * 知识点：
 *   DeepSeekApi         — 封装了 HTTP 请求，管理 API Key、Base URL
 *   DeepSeekChatOptions — 设定模型参数（model, temperature, maxTokens 等）
 *   DeepSeekChatModel   — 实现了 ChatModel 接口，是 Spring AI 的核心抽象
 *   ChatClient          — 高级 API，提供 prompt()、user()、call() 等链式调用
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
                // DeepSeek 默认就是 https://api.deepseek.com，
                // 但显式写上更清晰，也方便以后改成私有部署地址
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
     * 创建 DeepSeek ChatModel（核心 Bean）
     *
     * ChatModel 是 Spring AI 中最核心的接口，代表一个大语言模型。
     * DeepSeekChatModel 就是 ChatModel 的 DeepSeek 实现。
     * 后续 Phase 3 添加 Tool Calling 时，会用到这里的 ToolCallingManager。
     */
    @Bean
    public DeepSeekChatModel deepSeekChatModel(
            DeepSeekApi deepSeekApi,
            DeepSeekChatOptions deepSeekChatOptions
    ) {
        return DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(deepSeekChatOptions)
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.create())
                .build();
    }

    /**
     * 创建 ChatClient.Builder
     *
     * 这是 Spring AI 推荐的高级 API。
     * 在 Service 层注入 ChatClient.Builder，用它创建 ChatClient 实例。
     * 我们会在 CampusAIService 中通过 defaultSystem() 来设置校园知识库。
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel);
    }
}
