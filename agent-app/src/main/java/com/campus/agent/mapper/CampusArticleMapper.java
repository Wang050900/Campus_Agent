package com.campus.agent.mapper;

import com.campus.agent.entity.CampusArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CampusArticleMapper {
    List<CampusArticle> findAll();
    CampusArticle findById(@Param("id") Long id);
    List<CampusArticle> search(@Param("keyword") String keyword);
    int insert(CampusArticle article);
}
