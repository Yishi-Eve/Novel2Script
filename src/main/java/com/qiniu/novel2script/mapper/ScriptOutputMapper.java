package com.qiniu.novel2script.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiniu.novel2script.entity.ScriptOutput;
import com.qiniu.novel2script.enums.ScriptStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 剧本输出Mapper接口
 */
@Mapper
public interface ScriptOutputMapper extends BaseMapper<ScriptOutput> {

    /**
     * 根据小说ID查询剧本
     */
    ScriptOutput selectByNovelId(@Param("novelId") Long novelId);

    /**
     * 根据小说ID和状态查询剧本
     */
    ScriptOutput selectByNovelIdAndStatus(@Param("novelId") Long novelId, @Param("status") ScriptStatus status);

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

    /**
     * 更新YAML文件路径
     */
    @Update("UPDATE script_output SET yaml_file_path = #{yamlFilePath}, update_time = NOW() WHERE id = #{id}")
    int updateYamlFilePath(@Param("id") Long id, @Param("yamlFilePath") String yamlFilePath);

    /**
     * 更新全书概览文件路径
     */
    @Update("UPDATE script_output SET overview_file_path = #{overviewFilePath}, update_time = NOW() WHERE id = #{id}")
    int updateOverviewFilePath(@Param("id") Long id, @Param("overviewFilePath") String overviewFilePath);

    /**
     * 更新统计信息
     */
    @Update("UPDATE script_output SET total_chapters = #{totalChapters}, total_scenes = #{totalScenes}, update_time = NOW() WHERE id = #{id}")
    int updateStatistics(@Param("id") Long id, @Param("totalChapters") Integer totalChapters, @Param("totalScenes") Integer totalScenes);
}
