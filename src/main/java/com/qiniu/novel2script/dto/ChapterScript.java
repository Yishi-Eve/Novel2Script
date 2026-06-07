package com.qiniu.novel2script.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 章节剧本DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("章节剧本信息")
public class ChapterScript {

    @JsonProperty("episode_title")
    @Description("幕标题，简短概括本章内容")
    private String episodeTitle;

    @Description("角色列表")
    private List<Character> characters;

    @Description("场景列表")
    private List<Scene> scenes;
}
