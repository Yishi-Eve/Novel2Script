package com.qiniu.novel2script.enums;

import lombok.Getter;

@Getter
public enum NovelStatus {

    UPLOADED("已上传"),
    PARSING("解析中"),
    PARSED("解析完成"),
    ERROR("解析失败");

    private final String description;

    NovelStatus(String description) {
        this.description = description;
    }
}
