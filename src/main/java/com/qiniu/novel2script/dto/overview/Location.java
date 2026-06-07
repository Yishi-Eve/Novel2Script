package com.qiniu.novel2script.dto.overview;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 地点DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("地点信息")
public class Location {

    @Description("地点名称")
    private String name;

    @Description("地点描述")
    private String description;

    @Description("出现章节")
    private List<Integer> chapters;
}
