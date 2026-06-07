package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.overview.NovelOverview;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * 全书概览生成AI服务
 */
@AiService
public interface OverviewGenerator {

    /**
     * 生成全书概览
     *
     * @param chapters 章节内容
     * @return 全书概览
     */
    @SystemMessage(fromResource = "prompts/overview-system.txt")
    @UserMessage(fromResource = "prompts/overview-user.txt")
    NovelOverview generateOverview(@V("chapters") String chapters);
}
