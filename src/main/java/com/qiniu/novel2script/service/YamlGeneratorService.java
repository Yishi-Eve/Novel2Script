package com.qiniu.novel2script.service;

/**
 * YAML生成服务接口
 */
public interface YamlGeneratorService {

    /**
     * 生成YAML文件
     *
     * @param data     剧本数据（Map或对象）
     * @param filePath 文件路径
     * @return 文件路径
     */
    String generateYaml(Object data, String filePath);

    /**
     * 生成YAML字符串
     *
     * @param data 剧本数据
     * @return YAML字符串
     */
    String toYamlString(Object data);
}
