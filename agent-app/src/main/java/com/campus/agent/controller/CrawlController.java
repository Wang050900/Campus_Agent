package com.campus.agent.controller;

import com.campus.agent.service.ArticleCrawlerService;
import com.campus.agent.service.ArticleCrawlerService.CrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 官网文章抓取控制器
 */
@RestController
@RequestMapping("/crawl")
public class CrawlController {

    private static final Logger log = LoggerFactory.getLogger(CrawlController.class);

    private final ArticleCrawlerService crawlerService;

    public CrawlController(ArticleCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    /**
     * 手动触发全量抓取
     * GET /crawl/start
     */
    @GetMapping("/start")
    public ResponseEntity<Map<String, Object>> startCrawl() {
        log.info("校园官网文章抓取中...");
        try {
            CrawlResult result = crawlerService.crawlAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalNew", result.totalNew,
                    "errors", result.errors,
                    "details", result.categoryCounts,
                    "message", result.toString()
            ));
        } catch (Exception e) {
            log.error("抓取出错", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 启动时自动抓取
     * GET /crawl/start/init
     * 仅在第一次部署时使用
     */
    @GetMapping("/start/init")
    public ResponseEntity<Map<String, Object>> initCrawl() {
        log.info("初始化抓取（首次部署）...");
        try {
            CrawlResult result = crawlerService.crawlAll();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "初始化抓取完成: " + result.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
