package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件解析器抽象基类
 * 
 * 采用模板方法模式，定义解析的骨架流程：
 * 1. 获取文件大小
 * 2. 提取文本内容（由子类实现）
 * 3. 清洗文本（公共逻辑）
 * 4. 构建解析结果
 * 
 * 子类只需实现两个抽象方法：
 * - extractText(): 提取各自格式的文本内容
 * - isValidFileType(): 验证各自格式的文件有效性
 */
@Slf4j
public abstract class AbstractFileParser implements FileParser {

    /** 最大文件大小限制：10MB */
    protected static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 模板方法：解析文件并返回结果
     * 
     * 定义了解析的骨架流程，子类不需要重写此方法
     * 子类的差异逻辑通过 extractText() 方法实现
     */
    @Override
    public ParseResult parse(String filePath) throws FileParseException {
        try {
            Path path = Paths.get(filePath);
            // 获取文件大小
            long fileSize = Files.size(path);

            // 调用子类实现的文本提取方法
            String rawText = extractText(path);

            // 调用公共的文本清洗方法
            String cleanText = cleanText(rawText);

            // 构建解析结果
            return ParseResult.builder()
                .rawText(rawText)           // 原始文本
                .cleanText(cleanText)       // 清洗后的文本
                .fileType(getSupportedType()) // 文件类型
                .fileSize(fileSize)         // 文件大小
                .encoding(getEncoding())    // 文件编码
                .charCount(cleanText.length()) // 字符数
                .lineCount(countLines(cleanText)) // 行数
                .build();

        } catch (IOException e) {
            throw new FileParseException(getSupportedType().getDescription() + "文件解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证文件是否可以解析
     * 
     * 公共验证逻辑：
     * 1. 检查文件是否存在
     * 2. 检查文件大小是否超限
     * 3. 调用子类实现的类型特定验证
     */
    @Override
    public boolean isValid(String filePath) {
        try {
            Path path = Paths.get(filePath);

            // 检查文件是否存在
            if (!Files.exists(path)) {
                return false;
            }

            // 检查文件大小是否超过10MB
            if (Files.size(path) > MAX_FILE_SIZE) {
                return false;
            }

            // 调用子类实现的文件类型验证
            return isValidFileType(path);

        } catch (Exception e) {
            log.warn("文件验证异常: {}", filePath, e);
            return false;
        }
    }

    /**
     * 提取文本内容（抽象方法，由子类实现）
     * 
     * 各子类根据自己的文件格式实现不同的提取逻辑：
     * - TxtFileParser: 检测编码后读取纯文本
     * - MdFileParser: 将Markdown转换为纯文本
     * - DocxFileParser: 提取Word文档的段落和表格
     * 
     * @param path 文件路径
     * @return 提取的文本内容
     * @throws IOException 读取文件异常
     */
    protected abstract String extractText(Path path) throws IOException;

    /**
     * 验证文件类型是否有效（抽象方法，由子类实现）
     * 
     * 各子类根据自己的文件格式实现不同的验证逻辑：
     * - TxtFileParser: 检查是否为文本文件（非二进制）
     * - MdFileParser: 检查扩展名是否为.md或.markdown
     * - DocxFileParser: 检查扩展名并尝试打开验证
     * 
     * @param path 文件路径
     * @return 是否为有效的文件类型
     * @throws IOException 验证过程中的异常
     */
    protected abstract boolean isValidFileType(Path path) throws IOException;

    /**
     * 获取文件编码（可由子类重写）
     * 
     * 默认返回UTF-8，TxtFileParser会重写此方法返回检测到的实际编码
     * 
     * @return 编码名称
     */
    protected String getEncoding() {
        return "UTF-8";
    }

    /**
     * 公共文本清洗方法
     * 
     * 清洗步骤：
     * 1. 统一换行符：将Windows的\r\n和Mac的\r都转换为\n
     * 2. 去除多余空行：将连续3个以上的换行符替换为2个
     * 3. 去除每行首尾的空白字符
     * 4. 去除整个文本首尾的空白字符
     * 
     * @param text 原始文本
     * @return 清洗后的文本
     */
    protected String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // 步骤1：统一换行符
        String cleaned = text.replace("\r\n", "\n");  // Windows换行符
        cleaned = cleaned.replace("\r", "\n");         // Mac换行符

        // 步骤2：去除多余空行（保留最多2个连续换行）
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        // 步骤3：去除每行首尾空白
        String[] lines = cleaned.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].trim());
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        // 步骤4：去除整个文本首尾空白
        return sb.toString().trim();
    }

    /**
     * 统计文本行数
     * 
     * @param text 文本内容
     * @return 行数
     */
    protected int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n").length;
    }
}
