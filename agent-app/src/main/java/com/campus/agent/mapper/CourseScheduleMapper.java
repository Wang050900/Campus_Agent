package com.campus.agent.mapper;

import com.campus.agent.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CourseScheduleMapper {
    List<CourseSchedule> findByClassAndWeekday(
        @Param("className") String className,
        @Param("weekday") Integer weekday
    );
    List<CourseSchedule> findByClass(@Param("className") String className);
    List<String> findAllClassNames();
}
