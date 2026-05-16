package com.campus.agent.mapper;

import com.campus.agent.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ClassroomMapper {
    List<Classroom> findFreeByBuilding(
        @Param("building") String building
    );
    List<Classroom> findByBuilding(@Param("building") String building);
}
