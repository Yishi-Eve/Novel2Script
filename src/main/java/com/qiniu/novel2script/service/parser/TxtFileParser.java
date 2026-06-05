package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import com.qiniu.novel2script.service.util.EncodingDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class TxtFileParser implements FileParser {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final EncodingDetector encodingDetector;

    public TxtFileParser(EncodingDetector encodingDetector) {
        this.encodingDetector = encodingDetector;
    }

    @Override
    public FileType getSupportedType() {
        return FileType.TXT;
    }

    @Override
    public ParseResult parse(String filePath) throws FileParseException {
        try {
            Path path = Paths.get(filePath);
            long fileSize = Files.size(path);

            String encoding = encodingDetector.detect(path);
            log.debug("检测到文件编码: {}", encoding);

            String content = Files.readString(path, Charset.forName(encoding));

            String cleanContent = cleanText(content);

            return ParseResult.builder()
                .rawText(content)
                .cleanText(cleanContent)
                .fileType(FileType.TXT)
                .fileSize(fileSize)
                .encoding(encoding)
                .charCount(cleanContent.length())
                .lineCount(countLines(cleanContent))
                .build();

        } catch (IOException e) {
            throw new FileParseException("TXT文件解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isValid(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return false;
            }

            if (Files.size(path) > MAX_FILE_SIZE) {
                return false;
            }

            return isTextFile(path);

        } catch (IOException e) {
            log.warn("文件验证异常: {}", filePath, e);
            return false;
        }
    }

    private boolean isTextFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        int checkLength = Math.min(bytes.length, 8192);

        for (int i = 0; i < checkLength; i++) {
            byte b = bytes[i];
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.replace("\r\n", "\n");
        cleaned = cleaned.replace("\r", "\n");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        String[] lines = cleaned.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].trim());
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n").length;
    }
}
