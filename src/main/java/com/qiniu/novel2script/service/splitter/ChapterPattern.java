package com.qiniu.novel2script.service.splitter;

import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 章节模式定义
 * 定义各种章节标题的正则表达式模式
 */
@Getter
public enum ChapterPattern {

    /**
     * 中文章节格式
     * 匹配：第一章、第1章、第十二回、第一卷、第一篇等
     */
    CHINESE_CHAPTER(
            "中文章节",
            "^第[零一二三四五六七八九十百千万\\d]+[章回节卷集部篇].*",
            Pattern.MULTILINE
    ),

    /**
     * 英文章节格式
     * 匹配：Chapter 1、Chapter One等
     */
    ENGLISH_CHAPTER(
            "英文章节",
            "^Chapter\\s+\\d+.*",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    /**
     * 模式名称
     */
    private final String name;

    /**
     * 正则表达式
     */
    private final String regex;

    /**
     * 正则表达式标志
     */
    private final int flags;

    /**
     * 编译后的Pattern对象
     */
    private final Pattern pattern;

    ChapterPattern(String name, String regex, int flags) {
        this.name = name;
        this.regex = regex;
        this.flags = flags;
        this.pattern = Pattern.compile(regex, flags);
    }
}
