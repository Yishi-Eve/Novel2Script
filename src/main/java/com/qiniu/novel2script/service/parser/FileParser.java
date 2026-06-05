package com.qiniu.novel2script.service.parser;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;

public interface FileParser {
    //返回支持的filetype
    FileType getSupportedType();

    //解析文件
    ParseResult parse(String filePath) throws FileParseException;
    //验证有效
    boolean isValid(String filePath);
}
