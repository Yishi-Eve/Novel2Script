package com.qiniu.novel2script.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 章节摘要DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("章节摘要信息")
public class ChapterSummary {

    @Description("章节序号")
    private Integer chapterNumber;

    @Description("章节标题")
    private String title;

    @Description("主要角色")
    private List<String> characters;

    @Description("主要情节")
    private String plotSummary;

    @Description("关键场景")
    private List<String> keyScenes;

    @Description("重要对话")
    private List<String> keyDialogues;
}
