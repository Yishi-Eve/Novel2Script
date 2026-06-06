package com.qiniu.novel2script.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 章节DTO
 * 包含章节的基本信息和元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {

    /**
     * 章节序号（0=序章，1=第一章...）
     */
    private Integer chapterNumber;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节内容
     */
    private String content;

    /**
     * 字符数
     */
    private Integer charCount;

    /**
     * 行数
     */
    private Integer lineCount;

    /**
     * 在原文中的起始位置
     */
    private Long startPosition;

    /**
     * 在原文中的结束位置
     */
    private Long endPosition;

    /**
     * 计算并设置字符数和行数
     */
    public void calculateMetadata() {
        if (this.content != null) {
            this.charCount = this.content.length();
            this.lineCount = (int) this.content.chars().filter(c -> c == '\n').count() + 1;
        } else {
            this.charCount = 0;
            this.lineCount = 0;
        }
    }
}
