package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.service.util.EncodingDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TXT文件解析器
 * 
 * 负责解析纯文本文件（.txt），支持多种编码格式
 * 
 * 特点：
 * - 自动检测文件编码（UTF-8、GBK等）
 * - 验证文件是否为文本文件（非二进制）
 */
@Component
@Slf4j
public class TxtFileParser extends AbstractFileParser {

    /** 编码检测工具 */
    private final EncodingDetector encodingDetector;
    
    /** 检测到的文件编码 */
    private String detectedEncoding;

    public TxtFileParser(EncodingDetector encodingDetector) {
        this.encodingDetector = encodingDetector;
    }

    /**
     * 返回支持的文件类型：TXT
     */
    @Override
    public FileType getSupportedType() {
        return FileType.TXT;
    }

    /**
     * 提取TXT文件的文本内容
     * 
     * 流程：
     * 1. 使用EncodingDetector检测文件编码
     * 2. 使用检测到的编码读取文件内容
     * 
     * @param path 文件路径
     * @return 文件文本内容
     */
    @Override
    protected String extractText(Path path) throws IOException {
        // 检测文件编码
        this.detectedEncoding = encodingDetector.detect(path);
        log.debug("检测到文件编码: {}", detectedEncoding);
        
        // 使用检测到的编码读取文件
        return Files.readString(path, Charset.forName(detectedEncoding));
    }

    /**
     * 验证是否为有效的文本文件
     * 
     * 检查逻辑：读取文件前8KB，如果没有字节值为0，则认为是文本文件
     * 二进制文件通常包含值为0的字节
     * 
     * @param path 文件路径
     * @return 是否为文本文件
     */
    @Override
    protected boolean isValidFileType(Path path) throws IOException {
        return isTextFile(path);
    }

    /**
     * 返回检测到的文件编码
     * 
     * @return 编码名称（如UTF-8、GBK）
     */
    @Override
    protected String getEncoding() {
        return detectedEncoding != null ? detectedEncoding : "UTF-8";
    }

    /**
     * 判断文件是否为文本文件
     * 
     * 原理：二进制文件通常包含值为0的字节，文本文件不会
     * 只检查前8KB，提高性能
     * 
     * @param path 文件路径
     * @return 是否为文本文件
     */
    private boolean isTextFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        // 最多检查8KB
        int checkLength = Math.min(bytes.length, 8192);

        // 遍历字节，检查是否有值为0的字节
        for (int i = 0; i < checkLength; i++) {
            byte b = bytes[i];
            if (b == 0) {
                return false;  // 发现值为0的字节，是二进制文件
            }
        }
        return true;  // 没有值为0的字节，是文本文件
    }
}
