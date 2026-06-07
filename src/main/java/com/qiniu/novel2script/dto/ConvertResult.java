package com.qiniu.novel2script.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转换结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertResult {

    /**
     * 转换任务ID
     */
    private Long convertId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 状态消息
     */
    private String message;
}
