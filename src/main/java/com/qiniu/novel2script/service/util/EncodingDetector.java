package com.qiniu.novel2script.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class EncodingDetector {

    public String detect(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);

        String bomEncoding = detectBOM(bytes);
        if (bomEncoding != null) {
            return bomEncoding;
        }

        if (isValidUTF8(bytes)) {
            return "UTF-8";
        }

        if (isValidGBK(bytes)) {
            return "GBK";
        }

        log.warn("无法确定文件编码，默认使用UTF-8: {}", path);
        return "UTF-8";
    }

    private String detectBOM(byte[] bytes) {
        if (bytes.length >= 3) {
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return "UTF-8";
            }
        }
        if (bytes.length >= 2) {
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return "UTF-16LE";
            }
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }
        }
        return null;
    }

    private boolean isValidUTF8(byte[] bytes) {
        try {
            new String(bytes, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidGBK(byte[] bytes) {
        try {
            new String(bytes, Charset.forName("GBK"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
