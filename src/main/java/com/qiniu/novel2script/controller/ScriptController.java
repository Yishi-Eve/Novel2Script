package com.qiniu.novel2script.controller;

import com.qiniu.novel2script.dto.ConvertResult;
import com.qiniu.novel2script.dto.ConvertStatus;
import com.qiniu.novel2script.service.ScriptConvertService;
import com.qiniu.novel2script.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 剧本转换控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptConvertService scriptConvertService;

    /**
     * 触发转换
     */
    @PostMapping("/convert/{novelId}")
    public Result<ConvertResult> startConvert(@PathVariable Long novelId) {
        log.info("收到转换请求，小说ID：{}", novelId);
        ConvertResult result = scriptConvertService.startConvert(novelId);
        return Result.success(result);
    }

    /**
     * 获取转换状态
     */
    @GetMapping("/convert/{id}/status")
    public Result<ConvertStatus> getConvertStatus(@PathVariable Long id) {
        ConvertStatus status = scriptConvertService.getConvertStatus(id);
        return Result.success(status);
    }

    /**
     * 取消转换
     */
    @PostMapping("/convert/{id}/cancel")
    public Result cancelConvert(@PathVariable Long id) {
        log.info("收到取消转换请求，任务ID：{}", id);
        return scriptConvertService.cancelConvert(id);
    }

    /**
     * 重试转换
     */
    @PostMapping("/convert/{id}/retry")
    public Result<ConvertResult> retryConvert(@PathVariable Long id) {
        log.info("收到重试转换请求，任务ID：{}", id);
        ConvertResult result = scriptConvertService.retryConvert(id);
        return Result.success(result);
    }
}
