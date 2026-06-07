package com.qiniu.novel2script.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 转换状态DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertStatus {

    /**
     * 转换任务ID
     */
    private Long id;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 当前处理的章节
     */
    private Integer currentChapter;

    /**
     * 总章节数
     */
    private Integer totalChapters;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 实时转换日志
     */
    private List<String> logMessages;
}
