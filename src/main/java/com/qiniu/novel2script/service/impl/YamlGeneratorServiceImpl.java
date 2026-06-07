package com.qiniu.novel2script.service.impl;

import com.qiniu.novel2script.config.StorageProperties;
import com.qiniu.novel2script.service.YamlGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * YAML生成服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YamlGeneratorServiceImpl implements YamlGeneratorService {

    private final StorageProperties storageProperties;

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
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(1);
        options.setIndentWithIndicator(true);

        Yaml yaml = new Yaml(options);
        StringWriter writer = new StringWriter();
        yaml.dump(data, writer);
        return writer.toString();
    }
}
