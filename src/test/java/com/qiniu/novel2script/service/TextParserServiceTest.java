package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import com.qiniu.novel2script.service.impl.TextParserServiceImpl;
import com.qiniu.novel2script.service.parser.DocxFileParser;
import com.qiniu.novel2script.service.parser.FileParser;
import com.qiniu.novel2script.service.parser.MdFileParser;
import com.qiniu.novel2script.service.parser.TxtFileParser;
import com.qiniu.novel2script.service.util.EncodingDetector;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextParserServiceTest {

    private static TextParserService textParserService;

    @TempDir
    static Path tempDir;

    static Path testTxtPath;
    static Path testMdPath;
    static Path testDocxPath;

    @BeforeAll
    static void setUp() throws IOException {
        EncodingDetector encodingDetector = new EncodingDetector();
        TxtFileParser txtParser = new TxtFileParser(encodingDetector);
        MdFileParser mdParser = new MdFileParser();
        DocxFileParser docxParser = new DocxFileParser();

        List<FileParser> parsers = List.of(txtParser, mdParser, docxParser);
        textParserService = new TextParserServiceImpl(parsers);

        testTxtPath = tempDir.resolve("test.txt");
        Files.writeString(testTxtPath, "这是一个测试文件。\n第二行内容。\n第三行内容。");

        testMdPath = tempDir.resolve("test.md");
        Files.writeString(testMdPath, "# 标题\n\n这是正文内容。\n\n- 列表项1\n- 列表项2");

        testDocxPath = tempDir.resolve("test.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("这是第一段内容。");
            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("这是第二段内容。");
            Files.newOutputStream(testDocxPath);
            doc.write(Files.newOutputStream(testDocxPath));
        }
    }

    @Test
    void testParseTxtFile() {
        ParseResult result = textParserService.parse(testTxtPath.toString(), FileType.TXT);

        assertNotNull(result);
        assertEquals(FileType.TXT, result.getFileType());
        assertNotNull(result.getCleanText());
        assertTrue(result.getCharCount() > 0);
        assertTrue(result.getLineCount() > 0);
        assertNotNull(result.getParseTime());
        assertNotNull(result.getEncoding());
    }

    @Test
    void testParseMdFile() {
        ParseResult result = textParserService.parse(testMdPath.toString(), FileType.MD);

        assertNotNull(result);
        assertEquals(FileType.MD, result.getFileType());
        assertNotNull(result.getCleanText());
        assertTrue(result.getCharCount() > 0);
        assertTrue(result.getLineCount() > 0);
    }

    @Test
    void testParseDocxFile() {
        ParseResult result = textParserService.parse(testDocxPath.toString(), FileType.DOCX);

        assertNotNull(result);
        assertEquals(FileType.DOCX, result.getFileType());
        assertNotNull(result.getCleanText());
        assertTrue(result.getCharCount() > 0);
    }

    @Test
    void testParseNonExistentFile() {
        assertThrows(FileParseException.class, () -> {
            textParserService.parse("nonexistent.txt", FileType.TXT);
        });
    }

    @Test
    void testCanParse() {
        assertTrue(textParserService.canParse(testTxtPath.toString(), FileType.TXT));
        assertTrue(textParserService.canParse(testMdPath.toString(), FileType.MD));
        assertTrue(textParserService.canParse(testDocxPath.toString(), FileType.DOCX));
        assertFalse(textParserService.canParse("nonexistent.txt", FileType.TXT));
    }
}
