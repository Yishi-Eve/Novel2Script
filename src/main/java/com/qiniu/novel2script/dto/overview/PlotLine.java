package com.qiniu.novel2script.dto.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情节线DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("情节线信息")
public class PlotLine {

    @Description("情节线类型：主线/支线/伏笔")
    private String type;

    @Description("情节描述")
    private String description;

    @Description("涉及章节")
    private List<Integer> chapters;
}
