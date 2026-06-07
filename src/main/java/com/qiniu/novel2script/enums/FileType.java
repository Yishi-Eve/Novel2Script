package com.qiniu.novel2script.enums;

import lombok.Getter;

@Getter
public enum FileType {

    TXT("txt", "纯文本文件"),
    MD("md", "Markdown文件"),
    DOCX("docx", "Word文档");

    private final String extension;
    private final String description;

    FileType(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }

    public static FileType fromExtension(String extension) {
        for (FileType type : values()) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的文件类型: " + extension);
    }
}
