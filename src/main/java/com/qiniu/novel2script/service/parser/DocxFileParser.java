package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.enums.FileType;
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

/**
 * DOCX文件解析器
 * 
 * 负责解析Word文档（.docx），提取段落和表格中的文本内容
 * 
 * 特点：
 * - 使用Apache POI库解析DOCX格式
 * - 提取段落文本和表格文本
 * - 通过尝试打开文件验证DOCX格式有效性
 */
@Component
@Slf4j
public class DocxFileParser extends AbstractFileParser {

    /**
     * 返回支持的文件类型：DOCX
     */
    @Override
    public FileType getSupportedType() {
        return FileType.DOCX;
    }

    /**
     * 提取DOCX文件的文本内容
     * 
     * 流程：
     * 1. 使用XWPFDocument打开DOCX文件
     * 2. 遍历所有段落，提取段落文本
     * 3. 遍历所有表格，提取单元格文本
     * 4. 返回所有文本内容
     * 
     * @param path 文件路径
     * @return 提取的文本内容
     */
    @Override
    protected String extractText(Path path) throws IOException {
        StringBuilder content = new StringBuilder();

        // 使用try-with-resources自动关闭文档
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path))) {
            
            // 提取段落文本
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                // 只添加非空段落
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                }
            }

            // 提取表格文本
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        // 只添加非空单元格，用制表符分隔
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\t");
                        }
                    }
                    content.append("\n");  // 每行结束后换行
                }
            }
        }

        return content.toString();
    }

    /**
     * 验证是否为有效的DOCX文件
     * 
     * 检查逻辑：
     * 1. 检查文件扩展名是否为.docx
     * 2. 尝试用XWPFDocument打开文件，如果成功则有效
     * 
     * @param path 文件路径
     * @return 是否为有效的DOCX文件
     */
    @Override
    protected boolean isValidFileType(Path path) {
        // 检查文件扩展名
        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".docx")) {
            return false;
        }

        // 尝试打开文件验证格式
        return isValidDocxFile(path);
    }

    /**
     * 验证DOCX文件格式是否有效
     * 
     * 通过尝试用XWPFDocument打开文件来验证
     * 如果能成功打开，说明是有效的DOCX文件
     * 
     * @param path 文件路径
     * @return 是否为有效的DOCX文件
     */
    private boolean isValidDocxFile(Path path) {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path))) {
            return true;  // 能成功打开，是有效的DOCX文件
        } catch (Exception e) {
            log.debug("无效的DOCX文件: {}", path, e);
            return false;  // 无法打开，不是有效的DOCX文件
        }
    }
}
