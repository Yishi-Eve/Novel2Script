package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.ChapterScript;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ScriptConverter {

    @SystemMessage("你是一个有用的助手")
    String chat(@UserMessage String userMessage);

    /**
     * 转换章节为剧本
     *
     * @param overview        全书概览
     * @param contextChapters 前文上下文
     * @param title           章节标题
     * @param chapterNumber   章节序号
     * @param content         章节内容
     * @return 章节剧本
     */
    @SystemMessage(fromResource = "prompts/convert-system.txt")
    @UserMessage(fromResource = "prompts/convert-user.txt")
    ChapterScript convertChapter(
            @V("overview") String overview,
            @V("contextChapters") String contextChapters,
            @V("title") String title,
            @V("chapterNumber") int chapterNumber,
            @V("content") String content
    );
}
