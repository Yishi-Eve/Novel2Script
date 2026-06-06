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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本解析服务测试类
 * 
 * 包含正常情况测试和边界情况测试
 */
class TextParserServiceTest {

    private static TextParserService textParserService;

    @TempDir
    static Path tempDir;

    static Path testTxtPath;
    static Path testMdPath;
    static Path testDocxPath;

    @BeforeAll
    static void setUp() throws IOException {
        // 初始化解析器
        EncodingDetector encodingDetector = new EncodingDetector();
        TxtFileParser txtParser = new TxtFileParser(encodingDetector);
        MdFileParser mdParser = new MdFileParser();
        DocxFileParser docxParser = new DocxFileParser();

        // 创建服务实例
        List<FileParser> parsers = List.of(txtParser, mdParser, docxParser);
        textParserService = new TextParserServiceImpl(parsers);

        // 创建测试用的TXT文件
        testTxtPath = tempDir.resolve("test.txt");
        Files.writeString(testTxtPath, "这是一个测试文件。\n第二行内容。\n第三行内容。");

        // 创建测试用的MD文件
        testMdPath = tempDir.resolve("test.md");
        Files.writeString(testMdPath, "# 标题\n\n这是正文内容。\n\n- 列表项1\n- 列表项2");

        // 创建测试用的DOCX文件
        testDocxPath = tempDir.resolve("test.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("这是第一段内容。");
            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("这是第二段内容。");
            doc.write(Files.newOutputStream(testDocxPath));
        }
    }

    // ==================== 正常情况测试 ====================

    @Nested
    @DisplayName("正常情况测试")
    class NormalTests {

        @Test
        @DisplayName("解析TXT文件")
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
        @DisplayName("解析MD文件")
        void testParseMdFile() {
            ParseResult result = textParserService.parse(testMdPath.toString(), FileType.MD);

            assertNotNull(result);
            assertEquals(FileType.MD, result.getFileType());
            assertNotNull(result.getCleanText());
            assertTrue(result.getCharCount() > 0);
            assertTrue(result.getLineCount() > 0);
        }

        @Test
        @DisplayName("解析DOCX文件")
        void testParseDocxFile() {
            ParseResult result = textParserService.parse(testDocxPath.toString(), FileType.DOCX);

            assertNotNull(result);
            assertEquals(FileType.DOCX, result.getFileType());
            assertNotNull(result.getCleanText());
            assertTrue(result.getCharCount() > 0);
        }

        @Test
        @DisplayName("验证可解析文件")
        void testCanParse() {
            assertTrue(textParserService.canParse(testTxtPath.toString(), FileType.TXT));
            assertTrue(textParserService.canParse(testMdPath.toString(), FileType.MD));
            assertTrue(textParserService.canParse(testDocxPath.toString(), FileType.DOCX));
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class BoundaryTests {

        @Test
        @DisplayName("文件不存在 - 应抛出异常")
        void testParseNonExistentFile() {
            assertThrows(FileParseException.class, () -> {
                textParserService.parse("nonexistent.txt", FileType.TXT);
            });
        }

        @Test
        @DisplayName("验证不存在的文件 - 应返回false")
        void testCanParseNonExistentFile() {
            assertFalse(textParserService.canParse("nonexistent.txt", FileType.TXT));
        }

        @Test
        @DisplayName("空文件 - 应抛出异常或返回空结果")
        void testParseEmptyFile() throws IOException {
            // 创建空的TXT文件
            Path emptyTxt = tempDir.resolve("empty.txt");
            Files.writeString(emptyTxt, "");

            // 空文件可能抛出异常或返回空结果，取决于实现
            try {
                ParseResult result = textParserService.parse(emptyTxt.toString(), FileType.TXT);
                // 如果没有抛出异常，验证结果为空
                assertNotNull(result);
                assertEquals(0, result.getCharCount());
            } catch (FileParseException e) {
                // 抛出异常也是可接受的行为
                assertNotNull(e.getMessage());
            }
        }

        @Test
        @DisplayName("超大文件（超过10MB）- 应抛出异常")
        void testParseOversizedFile() throws IOException {
            // 创建一个超过10MB的文件
            Path largeFile = tempDir.resolve("large.txt");
            
            // 创建一个约11MB的文件
            // 每行 100 字节，需要 110000行 ≈ 11MB
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 110000; i++) {
                sb.append("这是一行测试文本，用于测试超大文件的处理能力。这是一行测试文本，用于测试超大文件。");
                sb.append("\n");
            }
            Files.writeString(largeFile, sb.toString());
            
            // 验证文件确实超过10MB
            long fileSize = Files.size(largeFile);
            assertTrue(fileSize > 10 * 1024 * 1024, "测试文件应大于10MB，实际大小: " + fileSize);

            // 应抛出异常
            assertThrows(FileParseException.class, () -> {
                textParserService.parse(largeFile.toString(), FileType.TXT);
            });
        }

        @Test
        @DisplayName("二进制文件伪装成TXT - 应抛出异常")
        void testParseBinaryFileAsTxt() throws IOException {
            // 创建一个包含二进制数据的文件，伪装成.txt
            Path binaryFile = tempDir.resolve("binary.txt");
            
            // 写入包含值为0的字节（二进制文件特征）
            byte[] binaryData = new byte[100];
            binaryData[0] = 'H';
            binaryData[1] = 'e';
            binaryData[2] = 'l';
            binaryData[3] = 'l';
            binaryData[4] = 'o';
            binaryData[5] = 0;  // 值为0的字节，二进制文件特征
            binaryData[6] = 'W';
            binaryData[7] = 'o';
            binaryData[8] = 'r';
            binaryData[9] = 'l';
            binaryData[10] = 'd';
            Files.write(binaryFile, binaryData);

            // 应抛出异常（因为不是有效的文本文件）
            assertThrows(FileParseException.class, () -> {
                textParserService.parse(binaryFile.toString(), FileType.TXT);
            });
        }

        @Test
        @DisplayName("MD文件扩展名验证 - .markdown扩展名")
        void testParseMarkdownExtension() throws IOException {
            // 创建.markdown扩展名的文件
            Path markdownFile = tempDir.resolve("test.markdown");
            Files.writeString(markdownFile, "# 标题\n\n这是内容。");

            // 应该能正常解析
            ParseResult result = textParserService.parse(markdownFile.toString(), FileType.MD);
            assertNotNull(result);
            assertEquals(FileType.MD, result.getFileType());
        }

        @Test
        @DisplayName("DOCX文件格式验证 - 无效的DOCX文件")
        void testParseInvalidDocxFile() throws IOException {
            // 创建一个假的DOCX文件（实际是文本文件）
            Path fakeDocx = tempDir.resolve("fake.docx");
            Files.writeString(fakeDocx, "这不是一个真正的DOCX文件");

            // 应抛出异常（因为不是有效的DOCX格式）
            assertThrows(FileParseException.class, () -> {
                textParserService.parse(fakeDocx.toString(), FileType.DOCX);
            });
        }

        @Test
        @DisplayName("不同编码的TXT文件 - GBK编码")
        void testParseGbkEncodedTxtFile() throws IOException {
            // 创建GBK编码的文件
            Path gbkFile = tempDir.resolve("test_gbk.txt");
            String content = "这是一个GBK编码的测试文件。\n第二行内容。";
            Files.write(gbkFile, content.getBytes("GBK"));

            // 应该能正常解析
            ParseResult result = textParserService.parse(gbkFile.toString(), FileType.TXT);
            assertNotNull(result);
            assertEquals(FileType.TXT, result.getFileType());
            assertNotNull(result.getEncoding());
            assertTrue(result.getCharCount() > 0);
        }

        @Test
        @DisplayName("特殊字符文件名")
        void testParseFileWithSpecialChars() throws IOException {
            // 创建包含特殊字符的文件名
            Path specialFile = tempDir.resolve("测试文件 (1).txt");
            Files.writeString(specialFile, "特殊字符文件名测试内容");

            // 应该能正常解析
            ParseResult result = textParserService.parse(specialFile.toString(), FileType.TXT);
            assertNotNull(result);
            assertEquals(FileType.TXT, result.getFileType());
        }

        @Test
        @DisplayName("只有换行符的文件")
        void testParseFileWithOnlyNewlines() throws IOException {
            // 创建只包含换行符的文件
            Path newlineFile = tempDir.resolve("newlines.txt");
            Files.writeString(newlineFile, "\n\n\n\n\n");

            // 应该能正常解析，但字符数为0（清洗后）
            ParseResult result = textParserService.parse(newlineFile.toString(), FileType.TXT);
            assertNotNull(result);
            // 清洗后可能为空或只有换行符
        }

        @Test
        @DisplayName("大量空行的文件")
        void testParseFileWithManyBlankLines() throws IOException {
            // 创建包含大量空行的文件
            Path blankLinesFile = tempDir.resolve("blank_lines.txt");
            StringBuilder sb = new StringBuilder();
            sb.append("第一行内容\n");
            for (int i = 0; i < 100; i++) {
                sb.append("\n");
            }
            sb.append("最后一行内容\n");
            Files.writeString(blankLinesFile, sb.toString());

            // 应该能正常解析，且空行被压缩
            ParseResult result = textParserService.parse(blankLinesFile.toString(), FileType.TXT);
            assertNotNull(result);
            // 清洗后空行应该被压缩
            assertTrue(result.getLineCount() < 100, "空行应该被压缩");
        }
    }
}
