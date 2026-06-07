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
     * 滑动窗口大小（已弃用，改用摘要设计）
     */
    private int windowSize = 3;

    /**
     * 摘要窗口大小（最近N章生成摘要）
     */
    private int summaryWindowSize = 3;

    /**
     * 概览最大token
     */
    private int overviewMaxTokens = 10000;

    /**
     * 摘要最大token
     */
    private int summaryMaxTokens = 2000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 超时时间（秒）
     */
    private int timeoutSeconds = 60;
}
