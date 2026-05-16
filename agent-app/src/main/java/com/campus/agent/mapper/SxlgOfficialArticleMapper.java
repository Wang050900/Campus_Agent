package com.campus.agent.mapper;

import com.campus.agent.entity.SxlgOfficialArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SxlgOfficialArticleMapper {
    /** 根据 URL 查重 */
    SxlgOfficialArticle findByUrl(@Param("url") String url);
    /** 插入新文章 */
    int insert(SxlgOfficialArticle article);
    /** 更新文章 */
    int update(SxlgOfficialArticle article);
    /** 查询所有 */
    List<SxlgOfficialArticle> findAll();
    /** 按分类查询 */
    List<SxlgOfficialArticle> findByCategory(@Param("category") String category);
    /** 搜索文章 */
    List<SxlgOfficialArticle> search(@Param("keyword") String keyword);
    /** 获取最新 N 条 */
    List<SxlgOfficialArticle> findLatest(@Param("limit") int limit);
}
