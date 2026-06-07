package com.qiniu.novel2script.dto.overview;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写作风格DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Description("写作风格信息")
public class WritingStyle {

    @JsonProperty("language_style")
    @Description("语言特点")
    private String languageStyle;

    @JsonProperty("narrative_style")
    @Description("叙事风格")
    private String narrativeStyle;

    @JsonProperty("dialogue_style")
    @Description("对话特点")
    private String dialogueStyle;
}
