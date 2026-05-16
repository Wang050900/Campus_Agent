-- ========================================
-- 绍兴理工学院官网文章抓取存储表
-- 数据来源：https://www.zsit.edu.cn
-- 时间范围：2025年6月至今
-- ========================================
CREATE TABLE IF NOT EXISTS `sxlg_official_article` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL COMMENT '文章标题',
  `url` VARCHAR(500) NOT NULL COMMENT '原文链接',
  `content` MEDIUMTEXT COMMENT '文章正文（HTML 净化后）',
  `summary` VARCHAR(500) COMMENT '文章摘要/简介',
  `category` VARCHAR(50) NOT NULL COMMENT '栏目：要闻/动态/科研/人物',
  `publish_date` DATE DEFAULT NULL COMMENT '发布日期',
  `author` VARCHAR(200) DEFAULT NULL COMMENT '作者/来源',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片 URL',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `crawl_status` VARCHAR(20) DEFAULT 'pending' COMMENT '抓取状态：pending/done/failed',
  `crawl_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_url` (`url`),
  INDEX `idx_category` (`category`),
  INDEX `idx_publish_date` (`publish_date`),
  INDEX `idx_crawl_status` (`crawl_status`),
  FULLTEXT INDEX `ft_search` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='绍兴理工学院官网文章';
