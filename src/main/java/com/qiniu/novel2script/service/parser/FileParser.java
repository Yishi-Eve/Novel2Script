package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;

/**
 * 文件解析器接口
 * 
 * 定义所有文件解析器的统一契约
 * 每种文件类型（TXT/MD/DOCX）都有对应的解析器实现
 */
public interface FileParser {

    /**
     * 获取此解析器支持的文件类型
     * 
     * @return 文件类型枚举（TXT/MD/DOCX）
     */
    FileType getSupportedType();

    /**
     * 解析文件并返回结果
     * 
     * @param filePath 文件路径
     * @return 解析结果，包含原始文本和清洗后的文本
     * @throws FileParseException 解析异常（文件不存在、格式错误等）
     */
    ParseResult parse(String filePath) throws FileParseException;

    /**
     * 验证文件是否有效
     * 
     * 检查内容包括：
     * - 文件是否存在
     * - 文件大小是否超限
     * - 文件格式是否正确
     * 
     * @param filePath 文件路径
     * @return 是否有效
     */
    boolean isValid(String filePath);
}
