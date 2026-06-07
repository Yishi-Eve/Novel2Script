package com.qiniu.novel2script.dto.overview;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("角色详细信息")
public class CharacterInfo {

    @Description("角色名称")
    private String name;

    @Description("角色描述")
    private String description;

    @Description("性格特点")
    private String personality;

    @Description("与其他角色关系")
    private String relationships;
}
