package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.ChapterSummary;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * 章节摘要生成AI服务
 */
@AiService
public interface SummaryGenerator {

    /**
     * 生成章节摘要
     *
     * @param chapterNumber 章节序号
     * @param title         章节标题
     * @param content       章节内容
     * @return 章节摘要
     */
    @SystemMessage(fromResource = "prompts/summary-system.txt")
    @UserMessage(fromResource = "prompts/summary-user.txt")
    ChapterSummary generateSummary(
            @V("chapterNumber") int chapterNumber,
            @V("title") String title,
            @V("content") String content
    );
}
