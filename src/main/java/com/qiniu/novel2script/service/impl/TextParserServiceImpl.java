package com.qiniu.novel2script.service.impl;

import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.exception.FileParseException;
import com.qiniu.novel2script.service.TextParserService;
import com.qiniu.novel2script.service.parser.FileParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TextParserServiceImpl implements TextParserService {

    private final Map<FileType, FileParser> parsers;

    public TextParserServiceImpl(List<FileParser> parserList) {
        this.parsers = parserList.stream()
            .collect(Collectors.toMap(
                FileParser::getSupportedType,
                Function.identity()
            ));
    }

    @Override
    public ParseResult parse(String filePath, FileType fileType) throws FileParseException {
        log.info("开始解析文件: {}, 类型: {}", filePath, fileType);
        //获取文件类型
        FileParser parser = parsers.get(fileType);
        if (parser == null) {
            throw new FileParseException("不支持的文件类型: " + fileType);
        }

        if (!parser.isValid(filePath)) {
            throw new FileParseException("文件验证失败: " + filePath);
        }

        long startTime = System.currentTimeMillis();
        ParseResult result = parser.parse(filePath);
        long endTime = System.currentTimeMillis();

        result.setParseTime(endTime - startTime);

        log.info("文件解析完成: {}, 耗时: {}ms, 字符数: {}",
            filePath, result.getParseTime(), result.getCharCount());

        return result;
    }

    @Override
    public boolean canParse(String filePath, FileType fileType) {
        FileParser parser = parsers.get(fileType);
        return parser != null && parser.isValid(filePath);
    }
}
