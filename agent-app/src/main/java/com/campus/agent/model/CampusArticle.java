package com.campus.agent.model;

/**
 * 校园文章模型
 * 代表从校园网上抓取的一篇文章，用于 RAG 检索
 */
public class CampusArticle {

    private String title;
    private String url;
    private String publishDate;
    private String content;

    public CampusArticle() {}

    public CampusArticle(String title, String url, String publishDate, String content) {
        this.title = title;
        this.url = url;
        this.publishDate = publishDate;
        this.content = content;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    /** 获取用于向量化的纯文本（标题+内容） */
    public String toEmbeddingText() {
        return "标题：" + title + "\n内容：" + content;
    }
}
