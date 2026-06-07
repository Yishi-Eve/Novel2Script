package com.qiniu.novel2script.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节标题DTO
 * 用于章节标题识别阶段，记录标题内容和位置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterTitle {

    /**
     * 标题文本
     */
    private String title;

    /**
     * 在文本中的起始位置
     */
    private Long position;

    /**
     * 匹配的模式名称
     */
    private String patternName;
}
