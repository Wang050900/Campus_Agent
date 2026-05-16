package com.campus.agent.service;

import com.campus.agent.entity.SxlgOfficialArticle;
import com.campus.agent.mapper.SxlgOfficialArticleMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 绍兴理工学院官网文章自动抓取服务
 *
 * 数据来源：https://www.zsit.edu.cn
 * 时间范围：2025年6月至今
 * 栏目覆盖：要闻、动态、科研、人物
 */
@Service
public class ArticleCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(ArticleCrawlerService.class);

    private static final String BASE_URL = "https://www.zsit.edu.cn";
    private static final LocalDate CUTOFF_DATE = LocalDate.of(2025, 6, 1);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 各栏目列表页配置 */
    private static final List<CategoryConfig> CATEGORIES = List.of(
            new CategoryConfig("要闻", BASE_URL + "/xwzx/xxyw.htm", "/xwzx/xxyw/", 1016, 207),
            new CategoryConfig("动态", BASE_URL + "/xwzx/xydt.htm", "/xwzx/xydt/", 1018, 187),
            new CategoryConfig("科研", BASE_URL + "/xwzx/xsky.htm", "/xwzx/xsky/", 1091, 30),
            new CategoryConfig("人物", BASE_URL + "/xwzx/slrw.htm", "/xwzx/slrw/", 1232, 10)
    );

    private final SxlgOfficialArticleMapper articleMapper;

    public ArticleCrawlerService(SxlgOfficialArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    /**
     * 执行全量抓取
     */
    public CrawlResult crawlAll() {
        log.info("========== 开始抓取绍兴理工学院官网文章 ==========");
        CrawlResult result = new CrawlResult();

        for (CategoryConfig cat : CATEGORIES) {
            try {
                int count = crawlCategory(cat);
                log.info("栏目「{}」抓取完成，新增 {} 篇", cat.name, count);
                result.totalNew += count;
                result.categoryCounts.put(cat.name, count);
            } catch (Exception e) {
                log.error("栏目「{}」抓取出错: {}", cat.name, e.getMessage());
                result.errors++;
            }
        }

        log.info("========== 抓取结束：新增 {} 篇，失败 {} 次 ==========", result.totalNew, result.errors);
        return result;
    }

    /**
     * 抓取某个栏目的所有分页
     */
    private int crawlCategory(CategoryConfig cat) throws Exception {
        int newCount = 0;
        int pageNum = 1;
        boolean hasMore = true;

        while (hasMore) {
            // 学校网站分页规律：第 1 页在 /xwzx/xxyw.htm，第 2 页起为 /xwzx/xxyw/(totalPages-pageNum+1).htm
            String listUrl = pageNum == 1 ? cat.firstPageUrl : (BASE_URL + cat.pagePrefix + (cat.totalPages - pageNum + 1) + ".htm");
            log.debug("抓取「{}」第 {} 页: {}", cat.name, pageNum, listUrl);

            try {
                Document listDoc = Jsoup.connect(listUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(10000)
                        .get();

                // 提取文章链接和日期
                List<ArticleLink> links = extractArticleLinks(listDoc, cat.categoryId);

                if (links.isEmpty()) {
                    hasMore = false;
                    break;
                }

                for (ArticleLink link : links) {
                    // 检查日期是否在范围内
                    if (link.date.isBefore(CUTOFF_DATE)) {
                        hasMore = false; // 后面的页面只会更旧
                        break;
                    }

                    // 检查是否已存在
                    if (articleMapper.findByUrl(link.url) != null) {
                        continue; // 已存在则跳过
                    }

                    // 获取详情页
                    try {
                        SxlgOfficialArticle article = fetchArticleDetail(link, cat.name);
                        if (article != null) {
                            articleMapper.insert(article);
                            newCount++;
                            log.info("  ✅ [{}] {} - {}", cat.name, link.date, link.title);
                        }
                    } catch (Exception e) {
                        log.warn("  ⚠️ 详情页抓取失败: {} - {}", link.url, e.getMessage());
                    }

                    // 适当休眠，避免被 ban
                    Thread.sleep(500);
                }

                pageNum++;
                // 防止死循环
                if (pageNum > MAX_CRAWL_PAGES) hasMore = false;

            } catch (Exception e) {
                log.warn("列表页抓取失败: {} - {}", listUrl, e.getMessage());
                hasMore = false;
            }
        }

        return newCount;
    }

    /**
     * 从列表页提取文章链接和日期
     *
     * 网站列表页结构（观察结果）：
     *   <li>
     *     <h3><a href="/info/1016/33357.htm">文章标题</a></h3>
     *     <a href="/info/1016/33357.htm">MM-dd</a>
     *     <a href="/info/1016/33357.htm">YYYY</a>
     *   </li>
     */
    private List<ArticleLink> extractArticleLinks(Document doc, int categoryId) {
        List<ArticleLink> links = new ArrayList<>();
        String expectedPrefix = "/info/" + categoryId + "/";
        Set<String> seen = new HashSet<>();

        // 策略 1: 找包含 /info/{categoryId}/ 的 <a> 标签，但需要过滤短文本（如 "05-15", "2026"）
        Elements allInfoLinks = doc.select("a[href*=" + expectedPrefix + "]");
        for (Element linkTag : allInfoLinks) {
            try {
                String href = linkTag.attr("href");
                String fullUrl = resolveUrl(href);

                if (seen.contains(fullUrl)) continue;

                // 检查标题是否有效（过滤日期链接如 "05-15"、"2026"）
                String title = linkTag.attr("title");
                if (title == null || title.isBlank()) {
                    title = linkTag.text().trim();
                }
                // 短文本（纯数字或 MM-dd 格式）不是标题
                if (title.length() < 5 || title.matches("\\d{2}-\\d{2}") || title.matches("20\\d{2}")) {
                    continue;
                }

                seen.add(fullUrl);

                // 提取日期：在父容器中查找 MM-dd + YYYY 组合
                LocalDate date = extractDateFromListItem(linkTag);

                if (date != null) {
                    links.add(new ArticleLink(title, fullUrl, date));
                }
            } catch (Exception e) {
                // skip broken items
            }
        }

        // 按日期从新到旧排序
        links.sort((a, b) -> b.date.compareTo(a.date));
        return links;
    }

    /**
     * 从列表项中提取日期（学校网站的特殊格式：MM-dd 和 YYYY 在不同元素中）
     */
    private LocalDate extractDateFromListItem(Element linkTag) {
        // 从 linkTag 所在的 <li> 容器中收集所有文本
        Element listItem = linkTag.closest("li");
        if (listItem == null) {
            // 尝试从最近的父容器获取
            Element parent = linkTag.parent();
            for (int i = 0; i < 3 && parent != null; i++) {
                if (!parent.tagName().equals("a") && !parent.tagName().equals("h1") && !parent.tagName().equals("h2") && !parent.tagName().equals("h3")) {
                    String text = parent.text();
                    LocalDate d = parseSplitDate(text);
                    if (d != null) return d;
                }
                parent = parent.parent();
            }
            return null;
        }

        // 从 <li> 中直接找年份和月日
        String liText = listItem.text();
        return parseSplitDate(liText);
    }

    /**
     * 解析学校官网的拆分日期格式
     * 网站上发布日期以 "MM-dd" 和 "2026" 分别放在不同元素中
     * 整个 <li> 的文本类似 "05-15 2026 文章标题"
     */
    private LocalDate parseSplitDate(String text) {
        if (text == null || text.isBlank()) return null;

        // 先尝试完整 yyyy-MM-dd 格式
        Pattern fullDate = Pattern.compile("(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})");
        Matcher fm = fullDate.matcher(text);
        if (fm.find()) {
            try {
                String y = fm.group(1);
                String m = String.format("%02d", Integer.parseInt(fm.group(2)));
                String d = String.format("%02d", Integer.parseInt(fm.group(3)));
                return LocalDate.parse(y + "-" + m + "-" + d, DATE_FORMATTER);
            } catch (Exception ignored) {}
        }

        // 匹配 MM-dd 和 YYYY 在不同位置的情况
        Pattern monthDay = Pattern.compile("(\\d{2})-(\\d{2})");
        Pattern year = Pattern.compile("(20\\d{2})");

        Matcher mdMatcher = monthDay.matcher(text);
        Matcher yMatcher = year.matcher(text);

        if (mdMatcher.find() && yMatcher.find()) {
            try {
                String y = yMatcher.group(1);
                String md = mdMatcher.group(0); // MM-dd
                return LocalDate.parse(y + "-" + md, DATE_FORMATTER);
            } catch (Exception ignored) {}
        }

        // 只匹配到 yyyy 的情况（降级使用）
        if (yMatcher.find()) {
            // 使用该年份的 6 月 1 日作为默认值？不好，用当前日期截断
            // 但如果没有月日，无法判断是否在 2025-06 之后，跳过
        }

        return null;
    }

    /** 从文本中解析发布日期（详情页使用） */
    private LocalDate parsePublishDate(String text) {
        if (text == null || text.isBlank()) return null;

        // 匹配 "发布时间：2026-05-15" 格式
        Pattern p = Pattern.compile("(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                String y = m.group(1);
                String month = String.format("%02d", Integer.parseInt(m.group(2)));
                String day = String.format("%02d", Integer.parseInt(m.group(3)));
                return LocalDate.parse(y + "-" + month + "-" + day, DATE_FORMATTER);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 抓取文章详情页
     */
    private SxlgOfficialArticle fetchArticleDetail(ArticleLink link, String category) {
        try {
            Document doc = Jsoup.connect(link.url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 提取标题
            String title = link.title;
            Element titleEl = doc.select("h1, h2, .article-title, .content-title, .xxy_text_title").first();
            if (titleEl != null) {
                title = titleEl.text().trim();
            }

            // 提取正文内容
            Element contentEl = doc.select(".article-content, .content, .xxy_text, #content, .wp_articlecontent, .v_news_content, .maincontent, .text-content").first();
            if (contentEl == null) {
                // 尝试提取所有正文相关的内容
                contentEl = doc.select("div.bt-content, div.news-content, div.main_cont, div.textbody").first();
            }
            if (contentEl == null) {
                // 最后手段：取 body 的主要段落
                contentEl = doc.body();
            }

            String fullContent = contentEl != null ? contentEl.text().trim() : "";
            if (fullContent.length() > 10) {
                // 移除干扰文本
                fullContent = fullContent.replaceAll("发布时间.*?点击次数.*?(\\d+)", "").trim();
            }

            // 提取摘要（前 200 字）
            String summary = fullContent.length() > 200 ? fullContent.substring(0, 200) + "..." : fullContent;

            // 提取作者
            String author = "";
            Element authorEl = doc.select(".article-author, .source, .info, .author, .xx_from").first();
            if (authorEl != null) {
                String authorText = authorEl.text().trim();
                if (authorText.contains("来源") || authorText.contains("作者")) {
                    // 截取 "来源：" 后面的内容
                    String[] parts = authorText.split("编辑|作者|来源");
                    author = parts.length > 1 ? parts[parts.length - 1].trim() : authorText;
                } else {
                    author = authorText;
                }
            }

            // 提取封面图
            String coverImage = "";
            Element imgEl = doc.select(".article-content img, .content img, .xxy_text img").first();
            if (imgEl != null) {
                String src = imgEl.attr("src");
                if (src != null && !src.isBlank()) {
                    coverImage = resolveUrl(src);
                }
            }

            // 提取发布时间（从详情页重新解析，更准确）
            LocalDate publishDate = link.date;
            String dateText = doc.select(".article-date, .time, .info, .date, .xxy_Time, .sj").text();
            if (!dateText.isBlank()) {
                LocalDate parsed = parsePublishDate(dateText);
                if (parsed != null) publishDate = parsed;
            }

            // 构建文章对象
            SxlgOfficialArticle article = new SxlgOfficialArticle();
            article.setTitle(title);
            article.setUrl(link.url);
            article.setContent(fullContent);
            article.setSummary(summary);
            article.setCategory(category);
            article.setPublishDate(publishDate);
            article.setAuthor(author.isEmpty() ? null : author);
            article.setCoverImage(coverImage.isEmpty() ? null : coverImage);
            article.setViewCount(0);
            article.setCrawlStatus("done");
            article.setCrawlTime(java.time.LocalDateTime.now());

            return article;

        } catch (Exception e) {
            log.warn("详情页抓取失败: {} - {}", link.url, e.getMessage());
            return null;
        }
    }

    // ===================================================================
    //  内部类
    // ===================================================================

    /** 将相对 URL 解析为完整 URL */
    private String resolveUrl(String href) {
        if (href == null || href.isBlank()) return "";
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        // 处理 ../ 开头的相对路径：向上回溯可能导致路径错误，直接清理
        String clean = href;
        while (clean.startsWith("../")) {
            clean = clean.substring(3);
        }
        while (clean.startsWith("./")) {
            clean = clean.substring(2);
        }
        if (clean.startsWith("/")) {
            return BASE_URL + clean;
        }
        return BASE_URL + "/" + clean;
    }

    /** 栏目配置 */
    private record CategoryConfig(String name, String firstPageUrl, String pagePrefix, int categoryId, int totalPages) {}
    private static final int MAX_CRAWL_PAGES = 50; // 每个栏目最多爬取页数

    /** 文章链接 */
    private record ArticleLink(String title, String url, LocalDate date) {}

    /** 抓取结果 */
    public static class CrawlResult {
        public int totalNew = 0;
        public int errors = 0;
        public Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        @Override
        public String toString() {
            return String.format("新增 %d 篇文章（失败 %d 次），各栏目：%s",
                    totalNew, errors, categoryCounts);
        }
    }
}
