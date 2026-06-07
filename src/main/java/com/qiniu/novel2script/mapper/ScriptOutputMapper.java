package com.qiniu.novel2script.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiniu.novel2script.entity.ScriptOutput;
import com.qiniu.novel2script.enums.ScriptStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 剧本输出Mapper接口
 */
@Mapper
public interface ScriptOutputMapper extends BaseMapper<ScriptOutput> {

    /**
     * 根据小说ID和状态查询剧本
     */
    ScriptOutput selectByNovelIdAndStatus(@Param("novelId") Long novelId, @Param("status") ScriptStatus status);

    /**
     * 根据小说ID查询最新的剧本
     */
    @Select("SELECT * FROM script_output WHERE novel_id = #{novelId} ORDER BY created_time DESC LIMIT 1")
    ScriptOutput selectByNovelId(@Param("novelId") Long novelId);

    /**
     * 更新转换进度
     */
    @Update("UPDATE script_output SET progress = #{progress}, current_chapter = #{currentChapter}, update_time = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("progress") Integer progress, @Param("currentChapter") Integer currentChapter);

    /**
     * 更新任务状态
     */
    @Update("UPDATE script_output SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") ScriptStatus status);

    /**
     * 更新状态和错误信息
     */
    @Update("UPDATE script_output SET status = #{status}, error_message = #{errorMessage}, update_time = NOW() WHERE id = #{id}")
    int updateStatusWithError(@Param("id") Long id, @Param("status") ScriptStatus status, @Param("errorMessage") String errorMessage);
}
