package com.campus.agent.model;

import java.util.List;

/**
 * 聊天响应体
 * 返回给前端的 JSON 结构
 */
public class ChatResponse {

    private String answer;
    private List<SourceInfo> sources;

    public ChatResponse() {}

    public ChatResponse(String answer, List<SourceInfo> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<SourceInfo> getSources() { return sources; }
    public void setSources(List<SourceInfo> sources) { this.sources = sources; }

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
}
