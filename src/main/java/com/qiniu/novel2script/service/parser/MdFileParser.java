package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Markdown文件解析器
 * 
 * 负责解析Markdown文件（.md、.markdown），提取纯文本内容
 * 
 * 特点：
 * - 使用CommonMark库解析Markdown语法
 * - 将Markdown转换为纯文本（去除标题符号、列表符号等）
 * - 默认使用UTF-8编码
 */
@Component
@Slf4j
public class MdFileParser extends AbstractFileParser {

    /** Markdown解析器（CommonMark库） */
    private final Parser markdownParser;

    public MdFileParser() {
        // 初始化Markdown解析器
        this.markdownParser = Parser.builder().build();
    }

    /**
     * 返回支持的文件类型：MD
     */
    @Override
    public FileType getSupportedType() {
        return FileType.MD;
    }

    /**
     * 提取Markdown文件的文本内容
     * 
     * 流程：
     * 1. 读取Markdown源码
     * 2. 使用CommonMark解析为AST（抽象语法树）
     * 3. 将AST渲染为纯文本
     * 
     * @param path 文件路径
     * @return 提取的纯文本内容
     */
    @Override
    protected String extractText(Path path) throws IOException {
        // 读取Markdown源码（UTF-8编码）
        String markdownContent = Files.readString(path, StandardCharsets.UTF_8);
        
        // 将Markdown转换为纯文本
        return parseMarkdownToText(markdownContent);
    }

    /**
     * 验证是否为有效的Markdown文件
     * 
     * 检查逻辑：文件扩展名是否为.md或.markdown
     * 
     * @param path 文件路径
     * @return 是否为Markdown文件
     */
    @Override
    protected boolean isValidFileType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".markdown");
    }

    /**
     * 将Markdown源码转换为纯文本
     * 
     * 转换示例：
     * - "# 标题" → "标题"
     * - "**粗体**" → "粗体"
     * - "- 列表项" → "列表项"
     * 
     * @param markdown Markdown源码
     * @return 纯文本内容
     */
    private String parseMarkdownToText(String markdown) {
        // 解析Markdown为AST（抽象语法树）
        Node document = markdownParser.parse(markdown);
        
        // 创建纯文本渲染器
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        
        // 将AST渲染为纯文本
        return renderer.render(document);
    }
}
