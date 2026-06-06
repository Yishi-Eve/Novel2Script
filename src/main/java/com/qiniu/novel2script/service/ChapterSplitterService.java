package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.ChapterSplitResult;
import com.qiniu.novel2script.dto.ChapterTitle;
import com.qiniu.novel2script.exception.ChapterSplitException;

import java.util.List;

/**
 * 章节分割服务接口
 * 负责将文本内容按照章节结构进行智能识别和分割
 */
public interface ChapterSplitterService {

    /**
     * 执行章节分割
     *
     * @param novelId  小说ID
     * @param cleanText 清洗后的文本内容（来自文本解析模块）
     * @return 分割结果
     * @throws ChapterSplitException 分割异常（文本为空、章节数不足等）
     */
    ChapterSplitResult splitChapters(Long novelId, String cleanText) throws ChapterSplitException;

    /**
     * 识别章节标题
     *
     * @param text 文本内容
     * @return 章节标题列表（包含位置信息）
     */
    List<ChapterTitle> detectChapterTitles(String text);

    /**
     * 从JSON文件加载章节
     *
     * @param filePath JSON文件路径
     * @return 章节列表
     * @throws ChapterSplitException 加载异常
     */
    List<Chapter> loadChapters(String filePath) throws ChapterSplitException;
}
