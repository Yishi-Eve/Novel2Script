package com.qiniu.novel2script.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件编码检测工具
 * 
 * 用于自动检测TXT文件的编码格式
 * 
 * 检测优先级：
 * 1. BOM标记（最准确）
 * 2. UTF-8解码尝试
 * 3. GBK解码尝试
 * 4. 默认UTF-8
 */
@Component
@Slf4j
public class EncodingDetector {

    /**
     * 检测文件编码
     * 
     * @param path 文件路径
     * @return 检测到的编码名称（如UTF-8、GBK、UTF-16LE等）
     * @throws IOException 读取文件异常
     */
    public String detect(Path path) throws IOException {
        // 读取文件所有字节
        byte[] bytes = Files.readAllBytes(path);

        // 优先检查BOM标记（最准确）
        String bomEncoding = detectBOM(bytes);
        if (bomEncoding != null) {
            return bomEncoding;
        }

        // 尝试UTF-8解码
        if (isValidUTF8(bytes)) {
            return "UTF-8";
        }

        // 尝试GBK解码
        if (isValidGBK(bytes)) {
            return "GBK";
        }

        // 无法确定编码，使用默认UTF-8
        log.warn("无法确定文件编码，默认使用UTF-8: {}", path);
        return "UTF-8";
    }

    /**
     * 检查BOM（字节顺序标记）
     * 
     * BOM是文件开头的特殊字节序列，用于标识编码格式：
     * - UTF-8 BOM: EF BB BF
     * - UTF-16 LE BOM: FF FE
     * - UTF-16 BE BOM: FE FF
     * 
     * @param bytes 文件字节数组
     * @return 检测到的编码名称，如果没有BOM返回null
     */
    private String detectBOM(byte[] bytes) {
        // 检查UTF-8 BOM（3字节）
        if (bytes.length >= 3) {
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return "UTF-8";
            }
        }
        // 检查UTF-16 BOM（2字节）
        if (bytes.length >= 2) {
            // UTF-16 Little Endian
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return "UTF-16LE";
            }
            // UTF-16 Big Endian
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }
        }
        return null;  // 没有BOM
    }

    /**
     * 验证是否为有效的UTF-8编码
     * 
     * 通过尝试用UTF-8解码来验证
     * 
     * @param bytes 文件字节数组
     * @return 是否为有效的UTF-8编码
     */
    private boolean isValidUTF8(byte[] bytes) {
        try {
            new String(bytes, StandardCharsets.UTF_8);
            return true;  // 解码成功
        } catch (Exception e) {
            return false;  // 解码失败
        }
    }

    /**
     * 验证是否为有效的GBK编码
     * 
     * 通过尝试用GBK解码来验证
     * 
     * @param bytes 文件字节数组
     * @return 是否为有效的GBK编码
     */
    private boolean isValidGBK(byte[] bytes) {
        try {
            new String(bytes, Charset.forName("GBK"));
            return true;  // 解码成功
        } catch (Exception e) {
            return false;  // 解码失败
        }
    }
}
