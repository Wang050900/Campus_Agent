package com.campus.agent.model;

import java.util.List;

/**
 * 聊天响应体
 * 返回给前端的 JSON 结构
 */
public class ChatResponse {

    /** 回答模式：RAG（检索增强）、TOOL（工具调用）、BASIC（基础问答） */
    private String mode;
    private String answer;
    private List<SourceInfo> sources;
    /** 工具调用信息，当 mode=TOOL 时不为空 */
    private List<ToolCallInfo> toolCalls;

    public ChatResponse() {}

    public ChatResponse(String answer, List<SourceInfo> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<SourceInfo> getSources() { return sources; }
    public void setSources(List<SourceInfo> sources) { this.sources = sources; }

    public List<ToolCallInfo> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCallInfo> toolCalls) { this.toolCalls = toolCalls; }

    /**
     * 文章来源信息
     */
    public static class SourceInfo {
        private String title;
        private String url;
        private String summary;

        public SourceInfo() {}

        public SourceInfo(String title, String url, String summary) {
            this.title = title;
            this.url = url;
            this.summary = summary;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    /**
     * 工具调用信息
     */
    public static class ToolCallInfo {
        private String toolName;
        private String description;
        private String result;

        public ToolCallInfo() {}

        public ToolCallInfo(String toolName, String description, String result) {
            this.toolName = toolName;
            this.description = description;
            this.result = result;
        }

        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
