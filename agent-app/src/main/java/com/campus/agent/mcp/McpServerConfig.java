package com.campus.agent.mcp;

import com.campus.agent.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP（Model Context Protocol）服务配置
 *
 * MCP 是 AI 与外部系统通信的开放协议。
 * 想象一下：你的 AI 助手需要查询校园数据，
 * 如果每个系统都自己写一套 API，会非常混乱。
 * MCP 就是"统一接口标准"——所有系统用同一种方式暴露功能和数据。
 *
 * 在我们的项目中，MCP 的落地方式是：
 *   1. CampusTools 通过 @Tool 注解暴露功能（工具的"定义"）
 *   2. ToolConfig 将这些工具注册到 ChatClient（工具的"注册"）
 *   3. AI 在对话中自动识别何时该调用工具（工具的"调用"）
 *   4. 工具返回实时数据，AI 据此回答（工具的"响应"）
 *
 * 这就构成了一个完整的 MCP 闭环！
 *
 * 🤖 Tool Calling = 让 AI 拥有"动手能力"
 * 🔌 MCP 协议 = 让工具调用变成标准化协议
 *
 * 以后对接真实校园系统时：
 *   教务系统 → MCP Server（查询课表/成绩）
 *   图书馆 → MCP Server（借书/预约座位）
 *   一卡通 → MCP Server（余额/消费记录）
 *
 * 全部通过统一的 MCP 协议暴露给 AI！
 */
@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    /**
     * 创建一个带 MCP 工具的 ChatClient
     * 这是一个"完整版"的 AI 客户端，拥有：
     *   ✅ RAG 检索能力
     *   ✅ Tool Calling 能力
     *   ✅ 校园知识库
     *
     * 当用户问"明天天气怎么样"时，AI 会：
     *   1. 分析问题 → "需要实时天气数据"
     *   2. 调用 getWeather() → 拿到实时数据
     *   3. 结合数据 → 生成回答
     *   整个过程对用户透明，用户只看到"小C回答了我的问题"✨
     */
    @Bean
    public ChatClient mcpChatClient(
            @Qualifier("ragChatClientBuilder") ChatClient.Builder ragChatClientBuilder,
            java.util.List<ToolCallback> campusToolCallbacks
    ) {
        return ragChatClientBuilder
                .defaultSystem("""
                        你是一位智能校园助手"小C"，拥有以下能力：
                        
                        1. 📚 知识问答：回答关于校园位置、流程、制度等问题
                        2. 🔧 工具调用：当需要实时数据时，调用对应的工具
                        3. 📄 文章检索：从校园网文章中查找相关信息
                        
                        可用的工具：
                        - getWeather：查询天气预报
                        - getCourseSchedule：查询课程安排
                        - getCampusNews：查询校园新闻
                        - findFreeClassroom：查找空闲教室
                        
                        回答规则：
                        - 如果需要实时数据，先调用工具再回答
                        - 如果工具有数据返回，基于数据回答
                        - 综合多个信息来源给出完整回答
                        """)
                .defaultToolCallbacks(campusToolCallbacks)
                .build();
    }
}
