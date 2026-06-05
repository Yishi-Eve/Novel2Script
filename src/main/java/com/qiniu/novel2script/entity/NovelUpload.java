package com.qiniu.novel2script.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qiniu.novel2script.enums.NovelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("novel_upload")
public class NovelUpload {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("stored_filename")
    private String storedFilename;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_type")
    private String fileType;

    @TableField("chapter_count")
    private Integer chapterCount;

    @TableField("status")
    private NovelStatus status;

    @TableField(value = "upload_time", fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
