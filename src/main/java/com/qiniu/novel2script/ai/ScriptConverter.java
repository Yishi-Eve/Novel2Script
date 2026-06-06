package com.qiniu.novel2script.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ScriptConverter {

    @SystemMessage("你是一个有用的助手")
    String chat(@UserMessage String userMessage);
}
