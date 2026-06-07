package com.qiniu.novel2script.service.splitter;

import com.qiniu.novel2script.dto.ChapterTitle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 章节标题检测器
 * 负责使用正则表达式识别文本中的章节标题
 */
@Component
@Slf4j
public class ChapterDetector {

    /**
     * 检测文本中的章节标题
     *
     * @param text 文本内容
     * @return 章节标题列表（按位置排序）
     */
    public List<ChapterTitle> detect(String text) {
        List<ChapterTitle> titles = new ArrayList<>();

        // 遍历所有预定义的章节模式
        for (ChapterPattern chapterPattern : ChapterPattern.values()) {
            Pattern pattern = chapterPattern.getPattern();
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String title = matcher.group().trim();
                long position = matcher.start();

                // 避免重复添加相同位置的标题
                boolean isDuplicate = titles.stream()
                        .anyMatch(t -> t.getPosition() == position);

                if (!isDuplicate) {
                    titles.add(ChapterTitle.builder()
                            .title(title)
                            .position(position)
                            .patternName(chapterPattern.getName())
                            .build());

                    log.debug("检测到章节标题: '{}' 位置: {} 模式: {}",
                            title, position, chapterPattern.getName());
                }
            }
        }

        // 按位置排序
        titles.sort(Comparator.comparingLong(ChapterTitle::getPosition));

        log.info("共检测到 {} 个章节标题", titles.size());
        return titles;
    }
}
