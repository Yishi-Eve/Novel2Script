package com.qiniu.novel2script.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("场景信息")
public class Scene {

    @JsonProperty("scene_number")
    @Description("场景序号")
    private Integer sceneNumber;

    @JsonProperty("scene_header")
    @Description("场景标题行，格式：地点 时间 内外景")
    private String sceneHeader;

    @Description("场景内容，包含描写和对话")
    private String content;
}
