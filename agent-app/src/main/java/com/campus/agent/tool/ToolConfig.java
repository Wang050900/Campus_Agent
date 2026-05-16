package com.campus.agent.tool;

import com.campus.agent.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 工具注册配置
 *
 * 把 @Tool 注解的方法注册到 ChatClient，
 * 这样 AI 就知道"我有这些工具可以用"。
 *
 * ToolCallAdvisor 是核心：
 *   它管理"AI 请求调用工具 → 我们执行 → 反馈结果"的循环
 *   让 AI 能像人一样"获取信息后再回答"
 */
@Configuration
public class ToolConfig {

    private static final Logger log = LoggerFactory.getLogger(ToolConfig.class);

    /**
     * 扫描所有 @Tool 方法，注册为 ToolCallback
     */
    @Bean
    public List<ToolCallback> campusToolCallbacks(CampusTools campusTools) {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(campusTools)
                .build();
        List<ToolCallback> callbacks = List.of(provider.getToolCallbacks());
        log.info("🔧 已注册 {} 个 AI 工具: getWeather, getCourseSchedule, getCampusNews, findFreeClassroom",
                callbacks.size());
        return callbacks;
    }

    /**
     * 带工具能力的 AI 问答 ChatClient
     *
     * ToolCallAdvisor 的工作流程：
     *   用户："明天北京天气怎么样？"
     *   1. AI 说："我需要调用 getWeather(北京, 2026-05-16)"
     *   2. ToolCallAdvisor 捕获这个请求
     *   3. 调用 getWeather 方法，拿到天气数据
     *   4. 把数据返回给 AI：{"temp": 28, "weather": "晴"}
     *   5. AI 基于数据回答："明天北京28°C，晴..."
     *
     *   整个过程对用户透明，看起来就像 AI 直接回答了！
     */
    @Bean
    public ChatClient toolChatClient(
            @Qualifier("chatClientBuilder") ChatClient.Builder chatClientBuilder,
            List<ToolCallback> campusToolCallbacks,
            ToolCallingManager toolCallingManager
    ) {
        return chatClientBuilder
                .defaultSystem("""
                        你是一位智能校园助手"小C"，拥有以下能力：
                        
                        🔧 可用工具（当需要实时数据时自动调用）：
                        1. getWeather(city, date) — 查询指定城市的天气预报
                        2. getCourseSchedule(studentClass, date) — 查询课程安排
                        3. getCampusNews(category) — 查询校园最新新闻
                        4. findFreeClassroom(building, timeSlot) — 查询空闲教室
                        
                        回答规则：
                        - 如果用户问到天气、课程、新闻、教室等实时信息，优先调用对应工具
                        - 先调用工具拿到数据，再用数据回答
                        - 如果工具不可用，用已有知识回答
                        - 回答简洁清晰，用中文
                        """)
                .defaultToolCallbacks(campusToolCallbacks)
                .defaultAdvisors(
                        // ToolCallAdvisor 管理"思考→行动→观察"循环
                        ToolCallAdvisor.builder()
                                .toolCallingManager(toolCallingManager)
                                .advisorOrder(0)  // 最高优先级
                                .build(),
                        // SimpleLoggerAdvisor 打印日志方便调试
                        new SimpleLoggerAdvisor()
                )
                .build();
    }
}
