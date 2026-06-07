package com.qiniu.novel2script.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("角色信息")
public class Character {

    @Description("角色名称")
    private String name;

    @Description("角色简短描述")
    private String description;
}
