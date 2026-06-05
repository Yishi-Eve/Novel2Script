package com.qiniu.novel2script.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private Path basePath = Paths.get("./uploads");
    private Path novelPath;
    private Path scriptPath;

    @PostConstruct
    public void init() {
        if (novelPath == null) {
            novelPath = basePath.resolve("novels");
        }
        if (scriptPath == null) {
            scriptPath = basePath.resolve("scripts");
        }
    }
}
