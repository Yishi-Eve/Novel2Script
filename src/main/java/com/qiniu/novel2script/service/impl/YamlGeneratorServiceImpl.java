package com.qiniu.novel2script.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.qiniu.novel2script.service.YamlGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * YAML生成服务实现 - 使用Jackson YAML
 */
@Slf4j
@Service
public class YamlGeneratorServiceImpl implements YamlGeneratorService {

    private final ObjectMapper yamlMapper;

    public YamlGeneratorServiceImpl() {
        YAMLFactory factory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
                .build();
        this.yamlMapper = new ObjectMapper(factory);
        this.yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generateYaml(Object data, String filePath) {
        try {
            String yamlContent = toYamlString(data);
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, yamlContent, StandardCharsets.UTF_8);
            log.info("YAML文件生成成功：{}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("YAML文件生成失败：{}", filePath, e);
            throw new RuntimeException("YAML文件生成失败", e);
        }
    }

    @Override
    public String toYamlString(Object data) {
        try {
            StringWriter writer = new StringWriter();
            yamlMapper.writeValue(writer, data);
            return writer.toString();
        } catch (IOException e) {
            log.error("YAML序列化失败", e);
            throw new RuntimeException("YAML序列化失败", e);
        }
    }
}
