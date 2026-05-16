package com.campus.agent.tool;

import com.campus.agent.entity.Classroom;
import com.campus.agent.entity.CourseSchedule;
import com.campus.agent.entity.CampusNews;
import com.campus.agent.mapper.ClassroomMapper;
import com.campus.agent.mapper.CourseScheduleMapper;
import com.campus.agent.mapper.CampusNewsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 校园工具集 — AI 实时调用数据库数据
 *
 * 数据来源：MySQL（通过 MyBatis Mapper 读取）
 *   - getWeather → 天气数据（来自外部 API / 模拟）
 *   - getCourseSchedule → 从 course_schedule 表读取
 *   - getCampusNews → 从 campus_news 表读取
 *   - findFreeClassroom → 从 classroom 表读取
 */
@Component
public class CampusTools {

    private static final Logger log = LoggerFactory.getLogger(CampusTools.class);

    private final CourseScheduleMapper courseScheduleMapper;
    private final CampusNewsMapper campusNewsMapper;
    private final ClassroomMapper classroomMapper;

    public CampusTools(CourseScheduleMapper courseScheduleMapper,
                       CampusNewsMapper campusNewsMapper,
                       ClassroomMapper classroomMapper) {
        this.courseScheduleMapper = courseScheduleMapper;
        this.campusNewsMapper = campusNewsMapper;
        this.classroomMapper = classroomMapper;
    }

    // ===================================================================
    //  工具 1：查询天气
    // ===================================================================

    @Tool(name = "getWeather", description = "查询指定城市的天气信息，包括温度、天气状况、湿度、风速等")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city,
            @ToolParam(description = "日期，格式：yyyy-MM-dd，例如：2026-05-16") String date
    ) {
        log.info("🌤️ 工具调用: getWeather(city={}, date={})", city, date);

        // 模拟数据（天气 API 需要第三方服务）
        java.util.Map<String, java.util.Map<String, Object>> mockData = java.util.Map.of(
                "北京", java.util.Map.of("temp", 28, "feelsLike", 30, "weather", "晴", "humidity", "45%", "wind", "南风 3级", "uv", "中等"),
                "上海", java.util.Map.of("temp", 25, "feelsLike", 27, "weather", "多云转阴", "humidity", "65%", "wind", "东南风 4级", "uv", "低"),
                "广州", java.util.Map.of("temp", 32, "feelsLike", 36, "weather", "雷阵雨", "humidity", "80%", "wind", "南风 2级", "uv", "高")
        );

        java.util.Map<String, Object> weather = mockData.getOrDefault(city,
                java.util.Map.of("temp", 25, "weather", "晴", "humidity", "50%", "wind", "微风")
        );

        String dayOfWeek = getDayOfWeekChinese(date);
        return String.format("""
                📍 %s %s（%s）天气预报：
                🌡️ 温度：%d°C（体感 %d°C）
                ☁️ 天气：%s
                💧 湿度：%s
                🌬️ 风力：%s
                ☀️ 紫外线：%s
                """, city, date, dayOfWeek,
                weather.get("temp"), weather.get("feelsLike"),
                weather.get("weather"), weather.get("humidity"),
                weather.get("wind"), weather.get("uv"));
    }

    // ===================================================================
    //  工具 2：查询课程表（数据库版）
    // ===================================================================

    @Tool(name = "getCourseSchedule", description = "查询指定日期和班级的课程安排，从数据库读取真实课表数据")
    public String getCourseSchedule(
            @ToolParam(description = "学生班级名称，例如：计科2024-1班") String studentClass,
            @ToolParam(description = "查询日期，格式：yyyy-MM-dd，例如：2026-05-16") String date
    ) {
        log.info("📚 工具调用: getCourseSchedule(class={}, date={})", studentClass, date);

        LocalDate queryDate;
        try {
            queryDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            queryDate = LocalDate.now();
        }

        int weekday = queryDate.getDayOfWeek().getValue();
        String dayOfWeek = getDayOfWeekChinese(date);

        // 从数据库查询课程
        List<CourseSchedule> courses = courseScheduleMapper.findByClassAndWeekday(studentClass, weekday);

        if (courses == null || courses.isEmpty()) {
            return String.format("📅 %s（%s）%s 当天没有课程安排，好好休息吧！", date, dayOfWeek, studentClass);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 %s（%s）%s 课程安排：\n\n", date, dayOfWeek, studentClass));

        for (int i = 0; i < courses.size(); i++) {
            CourseSchedule c = courses.get(i);
            String periodStr = switch (i) {
                case 0 -> "第一大节";
                case 1 -> "第二大节";
                case 2 -> "第三大节";
                case 3 -> "第四大节";
                case 4 -> "第五大节";
                default -> "第" + (i + 1) + "大节";
            };
            sb.append(String.format("  %s | %s\n", periodStr, c.getCourseName()));
            sb.append(String.format("     ⏰ %s-%s | 🏫 %s | 👨‍🏫 %s\n\n",
                    c.getStartTime(), c.getEndTime(), c.getLocation(), c.getTeacher()));
        }

        return sb.toString();
    }

    // ===================================================================
    //  工具 3：查询校园新闻（数据库版）
    // ===================================================================

    @Tool(name = "getCampusNews", description = "从数据库查询最新的校园新闻和通知公告，支持分类筛选：general(综合)、academic(学术)、activity(活动)、notice(通知)")
    public String getCampusNews(
            @ToolParam(description = "新闻分类：general(综合)、academic(学术)、activity(活动)、notice(通知)，不传则返回全部") String category
    ) {
        log.info("📰 工具调用: getCampusNews(category={})", category);

        List<CampusNews> newsList;
        if (category != null && !category.isBlank()) {
            newsList = campusNewsMapper.findByCategory(category);
        } else {
            newsList = campusNewsMapper.findAll();
        }

        if (newsList == null || newsList.isEmpty()) {
            return "📰 暂时没有相关新闻。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📰 %s最新校园新闻：\n\n",
                category != null && !category.isBlank() ? "「" + category + "」" : ""));

        for (CampusNews news : newsList) {
            sb.append(String.format("  📌 %s\n", news.getTitle()));
            String dateStr = news.getPublishDate() != null ? news.getPublishDate().toString() : "";
            String summary = news.getContent() != null && news.getContent().length() > 80
                    ? news.getContent().substring(0, 80) + "..."
                    : (news.getContent() != null ? news.getContent() : "");
            sb.append(String.format("     📅 %s | %s\n\n", dateStr, summary));
        }

        return sb.toString();
    }

    // ===================================================================
    //  工具 4：查询空闲教室（数据库版）
    // ===================================================================

    @Tool(name = "findFreeClassroom", description = "从数据库查询指定教学楼的空闲教室，用于自习或小组讨论")
    public String findFreeClassroom(
            @ToolParam(description = "教学楼名称，例如：教学楼、实验楼、逸夫楼") String building,
            @ToolParam(description = "要查询的时间段，例如：08:00-09:40") String timeSlot
    ) {
        log.info("🏫 工具调用: findFreeClassroom(building={}, timeSlot={})", building, timeSlot);

        List<Classroom> rooms = classroomMapper.findFreeByBuilding(building);

        if (rooms == null || rooms.isEmpty()) {
            return String.format("🏫 %s 当前没有空闲教室。", building);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏫 %s 在 %s 时空闲教室如下：\n\n", building, timeSlot));

        for (Classroom room : rooms) {
            String projector = room.getHasProjector() != null && room.getHasProjector() ? "📽️有投影" : "";
            String ac = room.getHasAc() != null && room.getHasAc() ? "❄️有空调" : "";
            String features = (projector + " " + ac).trim();
            sb.append(String.format("  ✅ %s%s%s%s\n",
                    building, room.getRoomNumber(),
                    room.getCapacity() != null ? "（" + room.getCapacity() + "人）" : "",
                    features.isEmpty() ? "" : " | " + features));
        }

        sb.append("\n💡 提示：直接去教室自习即可，无需预约");
        return sb.toString();
    }

    // ===================================================================
    //  辅助方法
    // ===================================================================

    private String getDayOfWeekChinese(String date) {
        try {
            LocalDate d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return switch (d.getDayOfWeek().getValue()) {
                case 1 -> "周一";
                case 2 -> "周二";
                case 3 -> "周三";
                case 4 -> "周四";
                case 5 -> "周五";
                case 6 -> "周六";
                case 7 -> "周日";
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }
}
