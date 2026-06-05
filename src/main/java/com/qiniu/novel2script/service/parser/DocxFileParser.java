package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class DocxFileParser implements FileParser {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public FileType getSupportedType() {
        return FileType.DOCX;
    }

    @Override
    public ParseResult parse(String filePath) throws FileParseException {
        try {
            Path path = Paths.get(filePath);
            long fileSize = Files.size(path);

            String content = extractTextFromDocx(path);

            String cleanContent = cleanText(content);

            return ParseResult.builder()
                .rawText(content)
                .cleanText(cleanContent)
                .fileType(FileType.DOCX)
                .fileSize(fileSize)
                .encoding("UTF-8")
                .charCount(cleanContent.length())
                .lineCount(countLines(cleanContent))
                .build();

        } catch (IOException e) {
            throw new FileParseException("DOCX文件解析失败: " + e.getMessage(), e);
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
            if (!fileName.endsWith(".docx")) {
                return false;
            }

            return isValidDocxFile(path);

        } catch (Exception e) {
            log.warn("文件验证异常: {}", filePath, e);
            return false;
        }
    }

    private String extractTextFromDocx(Path path) throws IOException {
        StringBuilder content = new StringBuilder();

        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\t");
                        }
                    }
                    content.append("\n");
                }
            }
        }

        return content.toString();
    }

    private boolean isValidDocxFile(Path path) {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path))) {
            return true;
        } catch (Exception e) {
            log.debug("无效的DOCX文件: {}", path, e);
            return false;
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.replace("\r\n", "\n"); //统一换行符
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n"); //去除多余空行

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
