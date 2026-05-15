package com.campus.agent.service;

import com.campus.agent.model.CampusArticle;
import com.campus.agent.model.ChatResponse.SourceInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务（关键词版本）
 *
 * 两个核心方法：
 *   search(question) → 找到相关文章，返回来源信息
 *   buildContext(question) → 构建 RAG Prompt 的上下文文本
 *
 * 真正的 AI 应用中，检索部分会用 EmbeddingModel + VectorStore
 * 但为了避免首次下载 90MB 的 ONNX 模型文件，
 * 我们用关键词匹配来模拟"检索"这一步。
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final List<CampusArticle> articles = new ArrayList<>();

    @Value("${campus.website.seed-data}")
    private Resource seedDataResource;

    @PostConstruct
    public void init() {
        try (InputStream in = seedDataResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, List<Map<String, String>>> data = yaml.load(in);
            List<Map<String, String>> articleMaps = data.get("articles");
            if (articleMaps == null) return;

            for (Map<String, String> map : articleMaps) {
                articles.add(new CampusArticle(
                        map.get("title"), map.get("url"),
                        map.get("publishDate"), map.get("content")
                ));
            }
            log.info("✅ RAG 加载了 {} 篇文章", articles.size());
        } catch (Exception e) {
            log.error("❌ 加载种子数据失败", e);
        }
    }

    /**
     * 搜索与问题相关的文章，返回来源信息
     * 如果没找到相关文章，返回空列表
     */
    public List<SourceInfo> search(String question) {
        List<ScoredArticle> scored = rankArticles(question);
        if (scored.isEmpty()) return Collections.emptyList();

        return scored.stream()
                .limit(3)
                .map(sa -> {
                    String summary = sa.article.getContent().length() > 150
                            ? sa.article.getContent().substring(0, 150) + "..."
                            : sa.article.getContent();
                    return new SourceInfo(sa.article.getTitle(), sa.article.getUrl(), summary);
                })
                .toList();
    }

    /**
     * 构建 RAG Prompt 上下文文本
     * 供 CampusAIService 拼接到发给 DeepSeek 的 Prompt 中
     */
    public String buildContext(String question) {
        List<ScoredArticle> scored = rankArticles(question);
        if (scored.isEmpty()) return "";

        return scored.stream()
                .limit(3)
                .map(sa -> "📄 【" + sa.article.getTitle() + "】\n" + sa.article.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 关键词匹配算法
     */
    private List<ScoredArticle> rankArticles(String question) {
        Set<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) return Collections.emptyList();

        List<ScoredArticle> results = new ArrayList<>();
        for (CampusArticle article : articles) {
            int score = 0;
            String titleLower = article.getTitle().toLowerCase();
            String contentLower = article.getContent().toLowerCase();

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
     * 
     * 对于中文，我们做两件事：
     *   1. 提取长度 >= 2 的词（按标点拆分后得到的）
     *   2. 生成连续 2 个字符的二元组（bigram）
     *      比如"体育选修课选课" → "体育" "育选" "选修" "修课" "课选" "选课"
     *      这样再匹配文章时，"体育"+"选修"+"选课"能精准命中
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

        // 1. 提取二元组 bigram
        for (int i = 0; i < cleaned.length() - 1; i++) {
            String bigram = cleaned.substring(i, i + 2);
            if (!stopWords.contains(bigram)) {
                keywords.add(bigram);
            }
        }

        // 2. 提取完整词（按标点拆分后长度 >= 2 的片段）
        String[] tokens = text.toLowerCase().split("\\p{P}|\\s+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 2 && !stopWords.contains(t)) {
                keywords.add(t);
            }
        }

        return keywords;
    }

    private record ScoredArticle(CampusArticle article, int score) {}
}
