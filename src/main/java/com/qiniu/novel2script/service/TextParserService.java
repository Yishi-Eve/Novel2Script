package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;

/**
 * 文本解析服务接口
 * 
 * 定义文本解析的核心方法，负责将不同格式的文件解析为统一的纯文本
 */
public interface TextParserService {

    /**
     * 解析文件并提取文本内容
     * 
     * @param filePath 文件路径
     * @param fileType 文件类型（TXT/MD/DOCX）
     * @return 解析结果，包含原始文本、清洗后文本等信息
     * @throws FileParseException 解析异常（文件不存在、格式错误等）
     */
    ParseResult parse(String filePath, FileType fileType) throws FileParseException;

    boolean canParse(String filePath, FileType fileType);
}
