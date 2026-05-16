package com.campus.agent.service;

import com.campus.agent.model.ChatResponse;
import com.campus.agent.model.ChatResponse.SourceInfo;
import com.campus.agent.model.ChatResponse.ToolCallInfo;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 校园 AI 助手 — 核心服务层
 *
 * === Phase 3 升级：集成 Tool Calling ===
 *
 * 现在 AI 同时拥有三种能力：
 *   ✅ 知识库问答（Phase 1）
 *   ✅ RAG 检索增强（Phase 2）
 *   ✅ Tool Calling 工具调用（Phase 3）
 *
 * 决策流程（askAI）：
 *   用户："明天北京天气怎么样？"
 *   1. 先查 RAG 是否有相关文章 → 没有
 *   2. 用工具 ChatClient 提问 → AI 识别需要调用 getWeather()
 *   3. getWeather("北京") 返回实时数据
 *   4. AI 基于数据回答："明天北京28°C，晴..."
 *
 * 用户："体育选课时间"
 *   1. 先查 RAG → 找到《体育选修课选课通知》
 *   2. 用 RAG ChatClient 提问 → 基于文章回答
 *   3. 返回回答 + 来源链接
 *
 * 用户："南食堂在哪"
 *   1. RAG 没找到 → 用基础知识库回答
 */
@Service
public class CampusAIService {

    private static final Logger log = LoggerFactory.getLogger(CampusAIService.class);

    private static final Pattern DATE_TIME_WEATHER_PATTERN = Pattern.compile(
            "(今天|明天|后天|昨天|星期|周[一二三四五六日天]|周几|星期几|几月几号|什么日期|" +
            "日期|时间|现在几点|几点了|天气|温度|气温|下雨|下雪|刮风|" +
            "摄氏度|预报|晴|阴|多云|常识|你好|你是谁|你能做什么|你会什么)",
            Pattern.CASE_INSENSITIVE
    );

    private final ChatClient chatClient;       // 基础问答
    private final ChatClient ragChatClient;     // RAG 问答
    private final ChatClient toolChatClient;    // 工具问答
    private final RagService ragService;

    public CampusAIService(
            @Qualifier("chatClientBuilder") ChatClient.Builder chatClientBuilder,
            @Qualifier("ragChatClientBuilder") ChatClient.Builder ragChatClientBuilder,
            @Qualifier("toolChatClient") ChatClient toolChatClient,
            RagService ragService,
            @Value("classpath:knowledge/campus-info.md") Resource campusInfoResource
    ) throws IOException {

        this.ragService = ragService;
        this.toolChatClient = toolChatClient;

        // 获取当前日期时间
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dayOfWeek = getChineseDayOfWeek();
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        // ---------- ChatClient 1：基础问答 ----------
        String campusKnowledge = StreamUtils.copyToString(
                campusInfoResource.getInputStream(), StandardCharsets.UTF_8
        );

        String basicSystemPrompt = """
                今天是 %s（%s），当前时间 %s。
                
                你是一位热情、专业的校园 AI 助手，名叫"小C"。
                你服务于 XX 大学的学生和教职工。

                ===== 校园知识库 =====
                %s
                ===== 知识库结束 =====

                回答要求：
                1. 基于上面的知识库回答问题
                2. 回答要简洁清晰，用中文
                3. 对于地点类问题，给出具体位置描述
                4. 对于流程类问题，给出步骤说明
                """.formatted(currentDate, dayOfWeek, currentTime, campusKnowledge);

        this.chatClient = chatClientBuilder
                .defaultSystem(basicSystemPrompt)
                .build();

        // ---------- ChatClient 2：RAG 问答 ----------
        String ragSystemPrompt = """
                你是一位严谨的校园信息助手"小C"。
                当前日期是 %s（%s）。

                你的工作流程：
                1. 用户问了一个问题
                2. 下面会提供"参考资料"
                3. 请**基于参考资料**回答用户的问题
                4. 先给一个 100 字以内的概括
                5. 然后列出详细信息
                6. 最后标注"具体文件："并列出参考文章的标题和 URL

                重要规则：
                - 如果参考资料与问题无关，使用你自己的知识回答
                - 综合多条参考资料的信息来回答
                """.formatted(currentDate, dayOfWeek);

        this.ragChatClient = ragChatClientBuilder
                .defaultSystem(ragSystemPrompt)
                .build();
    }

    /**
     * 问 AI 一个问题
     *
     * 智能决策链：
     *   1. 先判断是否为日期/天气/常识类问题 → 跳过RAG，直接走BASIC或TOOL
     *   2. 否则先查 RAG 是否有相关文章
     *   3. 有 → 用 RAG ChatClient 回答 (mode=RAG)
     *   4. 没有 → 用 Tool ChatClient（它会自动决定是否调用工具）(mode=TOOL)
     *   5. 工具也没用到 → 用基础知识库回答 (mode=BASIC)
     */
    public ChatResponse askAI(String question) {
        // 0. 快速路径：日期/天气/常识类问题 → 跳过RAG
        if (isDateOrGeneralQuestion(question)) {
            log.info("⚡ 检测到日期/天气/常识类问题，跳过RAG直接处理");
            // 如果是天气相关问题，优先使用 TOOL 模式（可调用 getWeather 工具）
            if (isWeatherQuestion(question)) {
                log.info("🌤️ 天气问题，使用 TOOL 模式");
                return askTool(question);
            }
            // 日期/时间/常识问题 → 基础问答（系统Prompt已包含当前日期）
            log.info("📖 日期/常识问题，使用 BASIC 模式");
            return askBasic(question);
        }

        // 1. 先尝试 RAG：搜索相关文章
        List<SourceInfo> sources = ragService.search(question);

        if (!sources.isEmpty()) {
            // RAG 模式：基于文章回答
            String context = ragService.buildContext(question);
            log.info("📚 使用 RAG 模式回答");
            String answer = ragChatClient.prompt()
                    .user(u -> u.text("""
                            用户问题：{question}
                            参考资料：{context}
                            请基于以上参考资料回答。
                            """)
                            .param("question", question)
                            .param("context", context))
                    .call()
                    .content();
            ChatResponse response = new ChatResponse(answer, sources);
            response.setMode("RAG");
            return response;
        }

        // 2. RAG 没找到 → 尝试 Tool Calling 模式
        return askTool(question);
    }

    /**
     * 使用 Tool Calling 模式回答
     */
    private ChatResponse askTool(String question) {
        log.info("🔧 尝试 Tool Calling 模式回答");
        try {
            String toolAnswer = toolChatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            // 检查是否真的有调用工具（返回内容里如果有工具提供的信息则是成功的）
            if (toolAnswer != null && !toolAnswer.isBlank()) {
                ChatResponse response = new ChatResponse(toolAnswer, null);
                // 通过问题关键词检测调用了哪些工具
                List<ToolCallInfo> detectedTools = detectToolCalls(question, toolAnswer);
                if (!detectedTools.isEmpty()) {
                    response.setMode("TOOL");
                    response.setToolCalls(detectedTools);
                    log.info("✅ 检测到工具调用: {}", detectedTools.stream()
                            .map(ToolCallInfo::getToolName).toList());
                } else {
                    response.setMode("BASIC");
                }
                return response;
            }
        } catch (Exception e) {
            log.warn("Tool calling 失败，回退到基础问答: {}", e.getMessage());
        }

        // 3. 降级：用基础知识库回答
        return askBasic(question);
    }

    /**
     * 使用基础知识库回答
     */
    private ChatResponse askBasic(String question) {
        log.info("📖 使用基础知识库回答");
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dayOfWeek = getChineseDayOfWeek();
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String answer = chatClient.prompt()
                .user(u -> u.text("""
                        用户问题：{question}
                        
                        注意：今天的真实日期是 {date}（{dayOfWeek}），当前时间 {time}。
                        如果用户询问日期、时间或相关常识，请直接利用以上信息回答，不需要查询任何工具。
                        """)
                        .param("question", question)
                        .param("date", currentDate)
                        .param("dayOfWeek", dayOfWeek)
                        .param("time", currentTime))
                .call()
                .content();
        ChatResponse response = new ChatResponse(answer, null);
        response.setMode("BASIC");
        return response;
    }

    /**
     * 判断是否为日期/时间/天气/常识类问题
     *
     * 这类问题不需要查 RAG 知识库，直接走 BASIC 或 TOOL 模式。
     */
    boolean isDateOrGeneralQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return DATE_TIME_WEATHER_PATTERN.matcher(question).find();
    }

    /**
     * 判断是否为天气相关问题
     */
    private boolean isWeatherQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String lower = question.toLowerCase();
        return lower.contains("天气") || lower.contains("温度") || lower.contains("气温")
                || lower.contains("下雨") || lower.contains("下雪") || lower.contains("刮风")
                || lower.contains("摄氏度") || lower.contains("预报") || lower.contains("湿度")
                || lower.contains("降水");
    }

    /**
     * 获取中文星期几
     */
    private static String getChineseDayOfWeek() {
        return switch (LocalDate.now().getDayOfWeek().getValue()) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "";
        };
    }

    /**
     * 通过关键词匹配，检测 AI 可能调用了哪些工具
     */
    private List<ToolCallInfo> detectToolCalls(String question, String answer) {
        List<ToolCallInfo> calls = new ArrayList<>();

        // 工具名称 → 关键词列表
        Map<String, String[]> toolKeywords = Map.of(
                "getWeather", new String[]{"天气", "温度", "湿度", "晴", "雨", "雪", "风", "气温", "降水"},
                "getCourseSchedule", new String[]{"课程", "课表", "上课", "课程安排", "课程表"},
                "getCampusNews", new String[]{"新闻", "通知", "公告", "校园新闻", "最新消息", "资讯"},
                "findFreeClassroom", new String[]{"空闲教室", "空教室", "可用教室", "自习室"}
        );

        // 工具中文名映射
        Map<String, String> toolNames = Map.of(
                "getWeather", "天气查询 \u2600\uFE0F",
                "getCourseSchedule", "课程查询 \uD83D\uDCC5",
                "getCampusNews", "校园新闻 \uD83D\uDCF0",
                "findFreeClassroom", "空闲教室 \uD83D\uDD0D"
        );

        // 工具描述映射
        Map<String, String> toolDescs = Map.of(
                "getWeather", "查询城市天气预报",
                "getCourseSchedule", "查询当日课程安排",
                "getCampusNews", "查询校园最新通知和活动",
                "findFreeClassroom", "查询教学楼空闲教室"
        );

        // 匹配：问题或回答中包含工具相关关键词
        for (Map.Entry<String, String[]> entry : toolKeywords.entrySet()) {
            String toolName = entry.getKey();
            for (String keyword : entry.getValue()) {
                if (question.contains(keyword) || (answer != null && answer.contains(keyword))) {
                    // 从回答中提取关键结果摘要（前 50 个字）
                    String resultSummary = null;
                    if (answer != null && answer.length() > 60) {
                        // 找到匹配关键词附近提取摘要
                        int idx = answer.indexOf(keyword);
                        if (idx >= 0) {
                            int start = Math.max(0, idx - 10);
                            int end = Math.min(answer.length(), idx + 50);
                            resultSummary = answer.substring(start, end).replace("\n", " ") + "...";
                        }
                    }
                    calls.add(new ToolCallInfo(
                            toolNames.getOrDefault(toolName, toolName),
                            toolDescs.getOrDefault(toolName, ""),
                            resultSummary
                    ));
                    break; // 一个工具只匹配一次
                }
            }
        }

        return calls;
    }
}
