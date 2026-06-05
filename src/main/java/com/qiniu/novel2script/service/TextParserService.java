package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;

public interface TextParserService {

    ParseResult parse(String filePath, FileType fileType) throws FileParseException;

    boolean canParse(String filePath, FileType fileType);
}
