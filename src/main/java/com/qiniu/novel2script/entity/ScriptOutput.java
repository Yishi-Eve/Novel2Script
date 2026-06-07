package com.qiniu.novel2script.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qiniu.novel2script.enums.ScriptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 剧本输出记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("script_output")
public class ScriptOutput {

    /**
     * 主键，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联小说ID
     */
    @TableField("novel_id")
    private Long novelId;

    /**
     * 剧本标题
     */
    @TableField("title")
    private String title;

    /**
     * 原作者
     */
    @TableField("original_author")
    private String originalAuthor;

    /**
     * 题材类型
     */
    @TableField("genre")
    private String genre;

    /**
     * YAML文件路径
     */
    @TableField("yaml_file_path")
    private String yamlFilePath;

    /**
     * 状态
     */
    @TableField("status")
    private ScriptStatus status;

    /**
     * 转换进度（0-100）
     */
    @TableField("progress")
    private Integer progress;

    /**
     * 当前处理章节
     */
    @TableField("current_chapter")
    private Integer currentChapter;

    /**
     * 总章节数
     */
    @TableField("total_chapters")
    private Integer totalChapters;

    /**
     * 总场景数
     */
    @TableField("total_scenes")
    private Integer totalScenes;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 全书概览文件路径
     */
    @TableField("overview_file_path")
    private String overviewFilePath;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
