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

/**
 * 文本解析服务实现类
 * 
 * 采用策略模式，根据文件类型自动选择对应的解析器
 * 
 * 工作原理：
 * 1. Spring自动注入所有FileParser实现（TxtFileParser、MdFileParser、DocxFileParser）
 * 2. 构造函数中构建 FileType -> FileParser 的映射表
 * 3. 解析时根据FileType从映射表中获取对应的解析器
 */
@Service
@Slf4j
public class TextParserServiceImpl implements TextParserService {

    /** 文件类型到解析器的映射表 */
    private final Map<FileType, FileParser> parsers;

    /**
     * Spring会自动注入所有FileParser实现，然后转换为Map
     * @param parserList Spring注入的所有解析器列表
     */
    public TextParserServiceImpl(List<FileParser> parserList) {
        // 将解析器列表转换为映射表，key为FileType，value为对应的解析器
        this.parsers = parserList.stream()
            .collect(Collectors.toMap(
                FileParser::getSupportedType,  // key: 解析器支持的类型
                Function.identity()            // value: 解析器本身
            ));
    }

    /**
     * 解析文件并返回结果
     * 
     * 流程：
     * 1. 根据文件类型获取对应的解析器
     * 2. 验证文件是否可以解析
     * 3. 调用解析器执行解析
     * 4. 记录解析耗时
     * 
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 解析结果
     * @throws FileParseException 解析异常
     */
    @Override
    public ParseResult parse(String filePath, FileType fileType) throws FileParseException {
        log.info("开始解析文件: {}, 类型: {}", filePath, fileType);

        // 步骤1：根据文件类型获取对应的解析器
        FileParser parser = parsers.get(fileType);
        if (parser == null) {
            throw new FileParseException("不支持的文件类型: " + fileType);
        }

        // 步骤2：验证文件是否可以解析
        if (!parser.isValid(filePath)) {
            throw new FileParseException("文件验证失败: " + filePath);
        }

        // 步骤3：调用解析器执行解析，并记录耗时
        long startTime = System.currentTimeMillis();
        ParseResult result = parser.parse(filePath);
        long endTime = System.currentTimeMillis();

        // 设置解析耗时
        result.setParseTime(endTime - startTime);

        log.info("文件解析完成: {}, 耗时: {}ms, 字符数: {}",
            filePath, result.getParseTime(), result.getCharCount());

        return result;
    }

    /**
     * 验证文件是否可以解析
     * 
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 是否可以解析
     */
    @Override
    public boolean canParse(String filePath, FileType fileType) {
        // 获取对应的解析器
        FileParser parser = parsers.get(fileType);
        // 解析器存在且文件验证通过
        return parser != null && parser.isValid(filePath);
    }
}
