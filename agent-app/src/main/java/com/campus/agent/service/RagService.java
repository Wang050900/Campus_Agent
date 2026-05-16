package com.campus.agent.service;

import com.campus.agent.entity.CampusArticle;
import com.campus.agent.entity.SxlgOfficialArticle;
import com.campus.agent.mapper.CampusArticleMapper;
import com.campus.agent.mapper.SxlgOfficialArticleMapper;
import com.campus.agent.model.ChatResponse.SourceInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务（数据库版）
 *
 * 数据来源：
 *   - MySQL campus_article 表（手动录入的校园资讯）
 *   - MySQL sxlg_official_article 表（自动抓取的官网文章）
 *
 * 两个核心方法：
 *   search(question) → 从数据库搜索相关文章
 *   buildContext(question) → 构建 RAG Prompt 的上下文
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final CampusArticleMapper campusArticleMapper;
    private final SxlgOfficialArticleMapper officialArticleMapper;
    private List<CampusArticle> articles = new ArrayList<>();
    private List<SxlgOfficialArticle> officialArticles = new ArrayList<>();

    public RagService(CampusArticleMapper campusArticleMapper,
                      SxlgOfficialArticleMapper officialArticleMapper) {
        this.campusArticleMapper = campusArticleMapper;
        this.officialArticleMapper = officialArticleMapper;
    }

    @PostConstruct
    public void init() {
        try {
            articles = campusArticleMapper.findAll();
            if (articles == null) articles = new ArrayList<>();
            log.info("✅ RAG 从 campus_article 加载了 {} 篇文章", articles.size());
        } catch (Exception e) {
            log.error("❌ 从 campus_article 加载文章失败", e);
            articles = new ArrayList<>();
        }
        try {
            officialArticles = officialArticleMapper.findAll();
            if (officialArticles == null) officialArticles = new ArrayList<>();
            log.info("✅ RAG 从 sxlg_official_article 加载了 {} 篇官网文章", officialArticles.size());
        } catch (Exception e) {
            log.error("❌ 从 sxlg_official_article 加载文章失败", e);
            officialArticles = new ArrayList<>();
        }
    }

    /**
     * 搜索与问题相关的文章，返回来源信息
     * 合并 campus_article 和 sxlg_official_article 的结果
     */
    public List<SourceInfo> search(String question) {
        List<ScoredArticle> campusResults = rankArticles(question);
        List<ScoredArticle> officialResults = rankOfficialArticles(question);

        List<ScoredArticle> merged = new ArrayList<>();
        if (!campusResults.isEmpty()) merged.addAll(campusResults);
        if (!officialResults.isEmpty()) merged.addAll(officialResults);
        merged.sort((a, b) -> b.score - a.score);

        if (merged.isEmpty()) return Collections.emptyList();

        return merged.stream()
                .limit(5)
                .map(sa -> {
                    String content = articleContent(sa.article);
                    String title = articleTitle(sa.article);
                    String url = articleUrl(sa.article);
                    String summary = content != null && content.length() > 150
                            ? content.substring(0, 150) + "..."
                            : (content != null ? content : "");
                    // 如果 URL 是相对路径，补全
                    if (url != null && !url.startsWith("http")) {
                        url = "https://www.zsit.edu.cn" + url;
                    }
                    return new SourceInfo(title, url, summary);
                })
                .toList();
    }

    /**
     * 构建 RAG Prompt 上下文文本
     */
    public String buildContext(String question) {
        StringBuilder sb = new StringBuilder();

        List<ScoredArticle> merged = new ArrayList<>();
        merged.addAll(rankArticles(question));
        merged.addAll(rankOfficialArticles(question));
        merged.sort((a, b) -> b.score - a.score);

        if (merged.isEmpty()) return "";

        int count = 0;
        for (ScoredArticle sa : merged) {
            if (count >= 5) break;
            sb.append("📄 【").append(articleTitle(sa.article)).append("】\n");
            sb.append(articleContent(sa.article)).append("\n\n---\n\n");
            count++;
        }

        return sb.toString();
    }

    // ===================================================================
    //  辅助方法：从 CampusArticle / SxlgOfficialArticle 提取字段
    // ===================================================================

    private static String articleTitle(Object article) {
        if (article instanceof CampusArticle ca) return ca.getTitle() != null ? ca.getTitle() : "";
        if (article instanceof SxlgOfficialArticle sa) return sa.getTitle() != null ? sa.getTitle() : "";
        return "";
    }

    private static String articleContent(Object article) {
        if (article instanceof CampusArticle ca) return ca.getContent() != null ? ca.getContent() : "";
        if (article instanceof SxlgOfficialArticle sa) return sa.getContent() != null ? sa.getContent() : "";
        return "";
    }

    private static String articleUrl(Object article) {
        if (article instanceof CampusArticle ca) return ca.getUrl() != null ? ca.getUrl() : "";
        if (article instanceof SxlgOfficialArticle sa) return sa.getUrl() != null ? sa.getUrl() : "";
        return "";
    }

    /**
     * 关键词匹配 campus_article
     */
    private List<ScoredArticle> rankArticles(String question) {
        if (articles == null || articles.isEmpty()) return Collections.emptyList();
        Set<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) return Collections.emptyList();

        List<ScoredArticle> results = new ArrayList<>();
        for (CampusArticle article : articles) {
            int score = 0;
            String titleLower = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
            String contentLower = article.getContent() != null ? article.getContent().toLowerCase() : "";
            for (String kw : keywords) {
                if (titleLower.contains(kw)) score += 3;
                if (contentLower.contains(kw)) score += 1;
            }
            if (score > 0) results.add(new ScoredArticle(article, score));
        }
        results.sort((a, b) -> b.score - a.score);
        return results;
    }

    /**
     * 关键词匹配 sxlg_official_article
     */
    private List<ScoredArticle> rankOfficialArticles(String question) {
        if (officialArticles == null || officialArticles.isEmpty()) return Collections.emptyList();
        Set<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) return Collections.emptyList();

        List<ScoredArticle> results = new ArrayList<>();
        for (SxlgOfficialArticle article : officialArticles) {
            int score = 0;
            String titleLower = article.getTitle() != null ? article.getTitle().toLowerCase() : "";
            String contentLower = article.getContent() != null ? article.getContent().toLowerCase() : "";
            for (String kw : keywords) {
                if (titleLower.contains(kw)) score += 3;
                if (contentLower.contains(kw)) score += 1;
            }
            if (score > 0) results.add(new ScoredArticle(article, score));
        }
        results.sort((a, b) -> b.score - a.score);
        return results;
    }

    /**
     * 中文关键词提取
     */
    private Set<String> extractKeywords(String text) {
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
                "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
                "你", "会", "着", "没有", "看", "好", "自己", "这", "他", "她",
                "它", "们", "什么", "怎么", "哪", "那", "为", "吗", "啊", "呢",
                "吧", "呀", "哦", "嗯", "啦", "地", "得", "能", "可以"
        );

        String cleaned = text.toLowerCase().replaceAll("\\p{P}|\\s+", "");
        Set<String> keywords = new HashSet<>();

        for (int i = 0; i < cleaned.length() - 1; i++) {
            String bigram = cleaned.substring(i, i + 2);
            if (!stopWords.contains(bigram)) keywords.add(bigram);
        }

        String[] tokens = text.toLowerCase().split("\\p{P}|\\s+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 2 && !stopWords.contains(t)) keywords.add(t);
        }

        return keywords;
    }

    private static class ScoredArticle {
        final Object article;
        final int score;
        ScoredArticle(Object article, int score) {
            this.article = article;
            this.score = score;
        }
    }
}
