package com.campus.agent.schedule;

import com.campus.agent.tool.CampusTools;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 每日定时推送服务
 *
 * 每天早上 8:00，小C 会自动查询当天的课程安排
 * 并推送到控制台（后续可以对接微信/邮件/短信推送）
 *
 * 使用 XXL-Job 替代 Spring @Scheduled：
 *   - dailyCoursePush: 每天早上 8:00 执行
 *   - weeklyOverview: 每周一早上 8:05 执行
 */
@Service
public class DailyPushService {

    private static final Logger log = LoggerFactory.getLogger(DailyPushService.class);

    private final ChatClient chatClient;
    private final CampusTools campusTools;

    public DailyPushService(
            @Qualifier("chatClientBuilder") ChatClient.Builder chatClientBuilder,
            CampusTools campusTools
    ) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一位贴心的课程提醒助手。用温暖、鼓励的语气向学生推送今日课程安排。")
                .build();
        this.campusTools = campusTools;
    }

    /**
     * 每天早上 8:00 准时推送今日课程
     *
     * XXL-Job 调度 cron = "0 0 8 * * ?"
     */
    @XxlJob("dailyCoursePush")
    public void dailyCoursePush() throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dayOfWeek = switch (LocalDate.now().getDayOfWeek().getValue()) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "";
        };

        log.info("⏰ 定时任务触发：每日课程推送 ({} {})", today, dayOfWeek);

        // 1. 用 CampusTools 获取课程数据
        String courseInfo = campusTools.getCourseSchedule("计科2024-1班", today);

        // 2. 让 AI 生成温暖的推送文案
        String pushMessage = chatClient.prompt()
                .user(u -> u.text("""
                        今天是 {date} {dayOfWeek}。
                        
                        今日课程信息：
                        {courseInfo}
                        
                        请用温暖鼓励的语气，向同学推送今日课程提醒。
                        开头可以加上一句早安问候。
                        """)
                        .param("date", today)
                        .param("dayOfWeek", dayOfWeek)
                        .param("courseInfo", courseInfo))
                .call()
                .content();

        // 3. 输出推送内容（后续可对接微信/邮件/钉钉等渠道）
        log.info("""
                
                ╔══════════════════════════════════════════╗
                ║          🌅 早安，新的一天开始了！         ║
                ╚══════════════════════════════════════════╝
                
                {}
                
                ═══════════════════════════════════════════
                """, pushMessage);
    }

    /**
     * 每周一早上 8:05 推送本周概览
     *
     * XXL-Job 调度 cron = "0 5 8 * * MON"
     */
    @XxlJob("weeklyOverview")
    public void weeklyOverview() throws Exception {
        log.info("📅 定时任务触发：本周课程概览（周一特供）");
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 汇总本周每天的课程
        StringBuilder weekInfo = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            String date = LocalDate.now().plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String courses = campusTools.getCourseSchedule("计科2024-1班", date);
            weekInfo.append("--- ").append(date).append(" ---\n").append(courses).append("\n");
        }

        String overview = chatClient.prompt()
                .user(u -> u.text("""
                        本周课程安排（从 {today} 开始）：
                        {weekInfo}
                        
                        请给同学一份简洁的本周课程概览，标注重点课程和提醒。
                        """)
                        .param("today", today)
                        .param("weekInfo", weekInfo.toString()))
                .call()
                .content();

        log.info("""
                
                ╔══════════════════════════════════════════╗
                ║       📅 本周课程概览（周一特供）          ║
                ╚══════════════════════════════════════════╝
                
                {}
                
                ═══════════════════════════════════════════
                """, overview);
    }
}
