package com.qiniu.novel2script.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScriptVO {
    private Long id;
    private Long novelId;
    private String title;
    private String status;
    private Integer progress;
    private Integer totalScenes;
    private String yamlContent;
    private LocalDateTime createdTime;
    private LocalDateTime updateTime;
}
