package com.qiniu.novel2script.dto;

import com.qiniu.novel2script.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {

    private String rawText; //原始内容

    private String cleanText; //清洗后的内容

    private FileType fileType; //文件类型

    private Long fileSize;

    private String encoding; //编码类型

    private Integer charCount; //字符数

    private Integer lineCount; //行数

    private Long parseTime;
}
