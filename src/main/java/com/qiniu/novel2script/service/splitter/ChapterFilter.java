package com.qiniu.novel2script.service.splitter;

import com.qiniu.novel2script.dto.ChapterTitle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 章节标题过滤器
 * 负责过滤误匹配的章节标题，如目录区域、过短/过长的标题等
 */
@Component
@Slf4j
public class ChapterFilter {

    /**
     * 最小标题长度
     */
    private static final int MIN_TITLE_LENGTH = 2;

    /**
     * 最大标题长度
     */
    private static final int MAX_TITLE_LENGTH = 50;

    /**
     * 中文章节标题模式：第X章、第X回、第X节等
     */
    private static final Pattern CHINESE_CHAPTER_PATTERN = 
            Pattern.compile("第[零一二三四五六七八九十百千万\\d]+[章回节卷集部篇]");

    /**
     * 中文第一章标题模式：第一章、第1章（精确匹配，不匹配第十一章）
     */
    private static final Pattern CHINESE_FIRST_CHAPTER_PATTERN = 
            Pattern.compile("^第[一1][章回节卷集部篇]");

    /**
     * 英文章节标题模式：Chapter X
     */
    private static final Pattern ENGLISH_CHAPTER_PATTERN = 
            Pattern.compile("Chapter\\s+\\d+", Pattern.CASE_INSENSITIVE);

    /**
     * 英文第一章标题模式：Chapter 1、Chapter One
     */
    private static final Pattern ENGLISH_FIRST_CHAPTER_PATTERN = 
            Pattern.compile("^Chapter\\s+(1|One)", Pattern.CASE_INSENSITIVE);

    /**
     * 过滤章节标题
     *
     * @param titles 原始标题列表
     * @return 过滤后的标题列表
     */
    public List<ChapterTitle> filter(List<ChapterTitle> titles) {
        if (titles == null || titles.isEmpty()) {
            return titles;
        }

        log.info("开始过滤章节标题，原始数量: {}", titles.size());

        // 步骤1：过滤目录区域
        List<ChapterTitle> filtered = filterDirectory(titles);

        // 步骤2：过滤过短/过长的标题
        filtered = filterByLength(filtered);

        log.info("过滤完成，剩余数量: {}", filtered.size());
        return filtered;
    }

    /**
     * 过滤目录区域
     * 检测重复出现的第一章标题，从重复位置开始截取（跳过目录）
     *
         * 目录区域的特征：会重复列出所有章节标题，正文开始时会再次出现第一个章节标题
     * 例如：
     *   [0] "第一章 初入江湖 .................. 1"  ← 目录
     *   [1] "第二章 奇遇 .................... 15"  ← 目录
     *   [2] "第一章 初入江湖"                      ← 正文开始
     *
     * @param titles 标题列表
     * @return 过滤后的标题列表
     */
    private List<ChapterTitle> filterDirectory(List<ChapterTitle> titles) {
        if (titles.size() < 3) {
            return titles;
        }

        for (int i = 0; i < titles.size() - 1; i++) {
            String currentTitle = titles.get(i).getTitle();
            
            // 检查是否是第一章的标题
            if (isFirstChapterTitle(currentTitle)) {
                // 提取标题核心部分（去掉页码、省略号等）
                String coreTitle = extractCoreTitle(currentTitle);
                
                // 查找后面是否有相同核心内容的标题
                for (int j = i + 1; j < titles.size(); j++) {
                    String laterCoreTitle = extractCoreTitle(titles.get(j).getTitle());
                    if (laterCoreTitle.equals(coreTitle)) {
                        // 找到重复标题，从j位置开始返回（跳过目录）
                        log.info("检测到目录区域，跳过前 {} 个标题，从 '{}' 开始", 
                                j, titles.get(j).getTitle());
                        return new ArrayList<>(titles.subList(j, titles.size()));
                    }
                }
            }
        }

        return titles;
    }

    /**
     * 判断是否是第一章的标题
     * 使用精确匹配，避免"第十一章"被误判为"第一章"
     *
     * @param title 标题文本
     * @return 是否是第一章的标题
     */
    private boolean isFirstChapterTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }

        // 中文：精确匹配"第一章"或"第1章"，不匹配"第十一章"
        if (CHINESE_FIRST_CHAPTER_PATTERN.matcher(title).find()) {
            return true;
        }

        // 英文：Chapter 1 或 Chapter One
        if (ENGLISH_FIRST_CHAPTER_PATTERN.matcher(title).find()) {
            return true;
        }

        return false;
    }

    /**
     * 提取标题的核心部分
     * 去掉页码、省略号、空格等修饰内容
     *
     * 例如：
     *   "第一章 初入江湖 .................. 1" → "第一章初入江湖"
     *   "第一章 初入江湖" → "第一章初入江湖"
     *
     * @param title 原始标题
     * @return 核心标题
     */
    private String extractCoreTitle(String title) {
        if (title == null) {
            return "";
        }

        // 去掉页码（数字开头或结尾的数字）
        String core = title.replaceAll("\\s*\\d+\\s*$", "");
        // 去掉省略号和点
        core = core.replaceAll("[.．…]+", "");
        // 去掉空格
        core = core.replaceAll("\\s+", "");
        // 去掉首尾空白
        core = core.trim();

        return core;
    }

    /**
     * 根据标题长度过滤
     * 过滤过短（<2字符）和过长（>50字符）的标题
     *
     * @param titles 标题列表
     * @return 过滤后的标题列表
     */
    private List<ChapterTitle> filterByLength(List<ChapterTitle> titles) {
        List<ChapterTitle> filtered = new ArrayList<>();

        for (ChapterTitle title : titles) {
            int length = title.getTitle().length();
            if (length >= MIN_TITLE_LENGTH && length <= MAX_TITLE_LENGTH) {
                filtered.add(title);
            } else {
                log.debug("过滤标题（长度不合规）: '{}' 长度: {}", title.getTitle(), length);
            }
        }

        return filtered;
    }
}
