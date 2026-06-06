package com.qiniu.novel2script.controller;

import com.qiniu.novel2script.ai.ScriptConverter;
import com.qiniu.novel2script.vo.Result;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ScriptConverter scriptConverter;

    @GetMapping("/test")
    public Result<String> test(@RequestParam String prompt) {
        String response = chatModel.chat(prompt);
        return Result.success(response);
    }

    @GetMapping("/test-service")
    public Result<String> testService(@RequestParam String prompt) {
        String response = scriptConverter.chat(prompt);
        return Result.success(response);
    }
}
