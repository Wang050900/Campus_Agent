package com.campus.agent.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampusArticle {
    private Long id;
    private String title;
    private String url;
    private String content;
    private String category;
    private LocalDate publishDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CampusArticle() {}
    // 保持和原来兼容的构造方法
    public CampusArticle(String title, String url, String publishDate, String content) {
        this.title = title;
        this.url = url;
        if (publishDate != null) this.publishDate = LocalDate.parse(publishDate);
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDate publishDate) { this.publishDate = publishDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
