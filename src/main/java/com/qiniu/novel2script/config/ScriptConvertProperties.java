package com.qiniu.novel2script.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 剧本转换配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "script-convert")
public class ScriptConvertProperties {

    /**
     * 摘要窗口大小（最近N章生成摘要）
     */
    private int summaryWindowSize = 3;
}
