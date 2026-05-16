package com.campus.agent.controller;

import com.campus.agent.tool.CampusTools;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP 协议接口演示
 *
 * MCP (Model Context Protocol) 是一种标准化协议，
 * 用于让 AI 模型与外部工具/数据源交互。
 *
 * 这个 Controller 演示了 MCP 协议的核心概念：
 *   GET  /mcp/tools        → 列出所有可用工具（List Tools）
 *   GET  /mcp/tools/{name} → 查看某个工具的详情
 *   POST /mcp/call         → 调用一个工具（Call Tool）
 *
 * 真实 MCP 协议使用 JSON-RPC 2.0 通信格式，
 * 这里简化为 REST API，便于理解和使用。
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private final CampusTools campusTools;

    public McpController(CampusTools campusTools) {
        this.campusTools = campusTools;
    }

    /**
     * 列出所有可用的 MCP 工具
     */
    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        return Map.of(
                "protocol", "Model Context Protocol (MCP) v1.0",
                "server", "Campus Agent MCP Server",
                "tools", Map.of(
                        "getWeather", Map.of(
                                "description", "查询天气",
                                "parameters", Map.of("city", "城市名称", "date", "日期")
                        ),
                        "getCourseSchedule", Map.of(
                                "description", "查询课程安排",
                                "parameters", Map.of("studentClass", "班级", "date", "日期")
                        ),
                        "getCampusNews", Map.of(
                                "description", "查询校园新闻",
                                "parameters", Map.of("category", "新闻分类")
                        ),
                        "findFreeClassroom", Map.of(
                                "description", "查询空闲教室",
                                "parameters", Map.of("building", "教学楼", "timeSlot", "时间段")
                        )
                )
        );
    }

    /**
     * 调用 MCP 工具
     * 请求体：{ "tool": "getWeather", "args": { "city": "北京", "date": "2026-05-16" } }
     */
    @PostMapping("/call")
    public Map<String, Object> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        Map<String, Object> args = (Map<String, Object>) request.getOrDefault("args", Map.of());

        String result = switch (toolName) {
            case "getWeather" -> campusTools.getWeather(
                    (String) args.getOrDefault("city", "北京"),
                    (String) args.getOrDefault("date", "2026-05-16")
            );
            case "getCourseSchedule" -> campusTools.getCourseSchedule(
                    (String) args.getOrDefault("studentClass", "计科2024-1班"),
                    (String) args.getOrDefault("date", "2026-05-16")
            );
            case "getCampusNews" -> campusTools.getCampusNews(
                    (String) args.getOrDefault("category", "general")
            );
            case "findFreeClassroom" -> campusTools.findFreeClassroom(
                    (String) args.getOrDefault("building", "教学楼"),
                    (String) args.getOrDefault("timeSlot", "08:00-09:40")
            );
            default -> "未知工具: " + toolName;
        };

        return Map.of(
                "tool", toolName,
                "result", result
        );
    }
}
