package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class MdFileParser implements FileParser {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final Parser markdownParser;

    public MdFileParser() {
        this.markdownParser = Parser.builder().build();
    }

    @Override
    public FileType getSupportedType() {
        return FileType.MD;
    }

    @Override
    public ParseResult parse(String filePath) throws FileParseException {
        try {
            Path path = Paths.get(filePath);
            long fileSize = Files.size(path);

            String markdownContent = Files.readString(path, StandardCharsets.UTF_8);

            String plainText = parseMarkdownToText(markdownContent);

            String cleanContent = cleanText(plainText);

            return ParseResult.builder()
                .rawText(markdownContent)
                .cleanText(cleanContent)
                .fileType(FileType.MD)
                .fileSize(fileSize)
                .encoding("UTF-8")
                .charCount(cleanContent.length())
                .lineCount(countLines(cleanContent))
                .build();

        } catch (IOException e) {
            throw new FileParseException("MD文件解析失败: " + e.getMessage(), e);
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

            String fileName = path.getFileName().toString().toLowerCase();
            return fileName.endsWith(".md") || fileName.endsWith(".markdown");

        } catch (IOException e) {
            log.warn("文件验证异常: {}", filePath, e);
            return false;
        }
    }

    private String parseMarkdownToText(String markdown) {
        Node document = markdownParser.parse(markdown);

        TextContentRenderer renderer = TextContentRenderer.builder()
            .build();

        return renderer.render(document);
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.replace("\r\n", "\n");
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
