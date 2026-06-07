package com.qiniu.novel2script.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 章节分割结果DTO
 * 包含完整的章节分割结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterSplitResult {

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 章节数量
     */
    private Integer chapterCount;

    /**
     * 章节列表
     */
    private List<Chapter> chapters;

    /**
     * JSON文件路径
     */
    private String chapterFilePath;

    /**
     * 分割耗时（毫秒）
     */
    private Long splitTime;
}
