package com.qiniu.novel2script.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NovelUploadVO {

    private Long id;
    private String originalFilename; //原始文件名
    private String storedFilename; //存储文件名
    private String fileType; //文件类型
    private Long fileSize; //文件大小
    private String fileSizeFormatted; //文件大小(可读形式)
    private String status;
    private LocalDateTime uploadTime;
}
