package com.qiniu.novel2script.dto.overview;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 全书概览DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("全书概览信息")
public class NovelOverview {

    @Description("角色表")
    private List<CharacterInfo> characters;

    @Description("情节线")
    private List<PlotLine> plotLines;

    @Description("地点表")
    private List<Location> locations;

    @Description("写作风格")
    private WritingStyle writingStyle;
}
