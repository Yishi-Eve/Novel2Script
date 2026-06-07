package com.qiniu.novel2script.controller;

import com.qiniu.novel2script.dto.ConvertResult;
import com.qiniu.novel2script.dto.ConvertStatus;
import com.qiniu.novel2script.entity.ScriptOutput;
import com.qiniu.novel2script.mapper.ScriptOutputMapper;
import com.qiniu.novel2script.service.ScriptConvertService;
import com.qiniu.novel2script.vo.Result;
import com.qiniu.novel2script.vo.ScriptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 剧本转换控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptConvertService scriptConvertService;
    private final ScriptOutputMapper scriptOutputMapper;

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

    /**
     * 根据小说ID获取剧本
     */
    @GetMapping("/script/novel/{novelId}")
    public Result<ScriptVO> getScriptByNovelId(@PathVariable Long novelId) {
        ScriptOutput scriptOutput = scriptOutputMapper.selectByNovelId(novelId);
        if (scriptOutput == null) {
            return Result.success(null);
        }

        ScriptVO.ScriptVOBuilder builder = ScriptVO.builder()
                .id(scriptOutput.getId())
                .novelId(scriptOutput.getNovelId())
                .title(scriptOutput.getTitle())
                .status(scriptOutput.getStatus().name())
                .progress(scriptOutput.getProgress())
                .totalScenes(scriptOutput.getTotalScenes())
                .createdTime(scriptOutput.getCreatedTime())
                .updateTime(scriptOutput.getUpdateTime());

        if (scriptOutput.getYamlFilePath() != null) {
            try {
                String yamlContent = Files.readString(Path.of(scriptOutput.getYamlFilePath()));
                builder.yamlContent(yamlContent);
            } catch (IOException e) {
                log.warn("读取YAML文件失败：{}", scriptOutput.getYamlFilePath(), e);
            }
        }

        return Result.success(builder.build());
    }

    /**
     * 下载剧本YAML文件
     */
    @GetMapping("/script/novel/{novelId}/download")
    public ResponseEntity<Resource> downloadScript(@PathVariable Long novelId) {
        ScriptOutput scriptOutput = scriptOutputMapper.selectByNovelId(novelId);
        if (scriptOutput == null || scriptOutput.getYamlFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path yamlPath = Path.of(scriptOutput.getYamlFilePath());
            if (!Files.exists(yamlPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(yamlPath);
            String filename = scriptOutput.getTitle() + ".yaml";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("下载剧本文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
