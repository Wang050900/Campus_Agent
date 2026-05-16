package com.campus.agent.mapper;

import com.campus.agent.entity.CampusNews;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CampusNewsMapper {
    List<CampusNews> findByCategory(@Param("category") String category);
    List<CampusNews> findAll();
    CampusNews findById(@Param("id") Long id);
}
