package com.campus.agent.config;

import org.springframework.context.annotation.Configuration;

/**
 * 定时任务配置
 *
 * XXL-Job 已替代 Spring @Scheduled，故注释掉 @EnableScheduling。
 * 所有定时任务交由 XXL-Job 调度中心统一管理。
 */
@Configuration
// @EnableScheduling  // 已迁移至 XXL-Job
public class SchedulingConfig {
}
