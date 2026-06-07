package com.qiniu.novel2script.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniu.novel2script.config.StorageProperties;
import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.ChapterSplitResult;
import com.qiniu.novel2script.dto.ChapterTitle;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.exception.ChapterSplitException;
import com.qiniu.novel2script.mapper.NovelUploadMapper;
import com.qiniu.novel2script.service.ChapterSplitterService;
import com.qiniu.novel2script.service.splitter.ChapterDetector;
import com.qiniu.novel2script.service.splitter.ChapterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 章节分割服务实现类
 * 负责将文本内容按照章节结构进行智能识别和分割
 */
@Service
@Slf4j
public class ChapterSplitterServiceImpl implements ChapterSplitterService {

    /**
     * 最小章节数
     */
    private static final int MIN_CHAPTER_COUNT = 3;

    private final ChapterDetector chapterDetector;
    private final ChapterFilter chapterFilter;
    private final NovelUploadMapper novelUploadMapper;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    public ChapterSplitterServiceImpl(ChapterDetector chapterDetector,
                                       ChapterFilter chapterFilter,
                                       NovelUploadMapper novelUploadMapper,
                                       StorageProperties storageProperties,
                                       ObjectMapper objectMapper) {
        this.chapterDetector = chapterDetector;
        this.chapterFilter = chapterFilter;
        this.novelUploadMapper = novelUploadMapper;
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行章节分割
     */
    @Override
    @Transactional
    public ChapterSplitResult splitChapters(Long novelId, String cleanText) throws ChapterSplitException {
        log.info("开始章节分割，小说ID: {}", novelId);
        long startTime = System.currentTimeMillis();

        // 验证文本内容
        if (cleanText == null || cleanText.trim().isEmpty()) {
            throw new ChapterSplitException("文本内容为空");
        }

        // 步骤1：识别章节标题
        List<ChapterTitle> titles = chapterDetector.detect(cleanText);
        if (titles.isEmpty()) {
            throw new ChapterSplitException("未识别到任何章节标题");
        }

        // 步骤2：过滤误匹配
        titles = chapterFilter.filter(titles);
        if (titles.isEmpty()) {
            throw new ChapterSplitException("过滤后无有效章节标题");
        }

        // 步骤3：分割章节内容
        List<Chapter> chapters = splitContent(titles, cleanText);

        // 步骤4：验证章节数量
        if (chapters.size() < MIN_CHAPTER_COUNT) {
            throw new ChapterSplitException(
                    String.format("章节数量不足，至少需要%d个章节，当前仅识别到%d个",
                            MIN_CHAPTER_COUNT, chapters.size()));
        }

        // 步骤5：生成章节元数据
        for (Chapter chapter : chapters) {
            chapter.calculateMetadata();
        }

        // 步骤6：保存到JSON文件
        String filePath = saveToFile(novelId, chapters);

        // 步骤7：更新数据库
        updateNovelUpload(novelId, chapters.size(), filePath);

        long endTime = System.currentTimeMillis();
        long splitTime = endTime - startTime;

        log.info("章节分割完成，小说ID: {}, 章节数: {}, 耗时: {}ms", novelId, chapters.size(), splitTime);

        return ChapterSplitResult.builder()
                .novelId(novelId)
                .chapterCount(chapters.size())
                .chapters(chapters)
                .chapterFilePath(filePath)
                .splitTime(splitTime)
                .build();
    }

    /**
     * 识别章节标题
     */
    @Override
    public List<ChapterTitle> detectChapterTitles(String text) {
        List<ChapterTitle> titles = chapterDetector.detect(text);
        return chapterFilter.filter(titles);
    }

    /**
     * 从JSON文件加载章节
     */
    @Override
    public List<Chapter> loadChapters(String filePath) throws ChapterSplitException {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                throw new ChapterSplitException("章节文件不存在: " + filePath);
            }

            String json = Files.readString(path, StandardCharsets.UTF_8);
            ChapterSplitResult result = objectMapper.readValue(json, new TypeReference<>() {});
            return result.getChapters();
        } catch (IOException e) {
            throw new ChapterSplitException("加载章节文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分割章节内容
     *
     * @param titles 标题列表
     * @param text   原始文本
     * @return 章节列表
     */
    private List<Chapter> splitContent(List<ChapterTitle> titles, String text) {
        List<Chapter> chapters = new ArrayList<>();

        // 处理标题前的内容（序章）
        if (!titles.isEmpty() && titles.get(0).getPosition() > 0) {
            String preContent = text.substring(0, titles.get(0).getPosition().intValue()).trim();
            if (!preContent.isEmpty()) {
                chapters.add(Chapter.builder()
                        .chapterNumber(0)
                        .title("序章")
                        .content(preContent)
                        .startPosition(0L)
                        .endPosition(titles.get(0).getPosition())
                        .build());
            }
        }

        // 分割各章节
        for (int i = 0; i < titles.size(); i++) {
            int start = titles.get(i).getPosition().intValue();
            int end = (i < titles.size() - 1) ? titles.get(i + 1).getPosition().intValue() : text.length();

            String title = titles.get(i).getTitle();
            String content = text.substring(start + title.length(), end).trim();

            chapters.add(Chapter.builder()
                    .chapterNumber(i + 1)
                    .title(title)
                    .content(content)
                    .startPosition((long) start)
                    .endPosition((long) end)
                    .build());
        }

        return chapters;
    }

    /**
     * 保存章节到JSON文件
     *
     * @param novelId  小说ID
     * @param chapters 章节列表
     * @return 文件路径
     */
    private String saveToFile(Long novelId, List<Chapter> chapters) {
        try {
            // 生成文件名
            String fileName = "novel_" + novelId + "_chapters.json";

            // 获取章节存储路径
            Path chapterPath = storageProperties.getChapterPath();
            Files.createDirectories(chapterPath);

            // 拼接完整路径
            Path filePath = chapterPath.resolve(fileName);

            // 构建结果对象
            ChapterSplitResult result = ChapterSplitResult.builder()
                    .novelId(novelId)
                    .chapterCount(chapters.size())
                    .chapters(chapters)
                    .chapterFilePath(filePath.toString())
                    .build();

            // 序列化为JSON并写入文件
            String json = objectMapper.writeValueAsString(result);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);

            log.info("章节文件已保存: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new ChapterSplitException("保存章节文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新数据库中的小说记录
     *
     * @param novelId        小说ID
     * @param chapterCount   章节数量
     * @param chapterFilePath 章节文件路径
     */
    private void updateNovelUpload(Long novelId, int chapterCount, String chapterFilePath) {
        NovelUpload novelUpload = novelUploadMapper.selectById(novelId);
        if (novelUpload == null) {
            throw new ChapterSplitException("小说记录不存在: " + novelId);
        }

        novelUpload.setChapterCount(chapterCount);
        novelUpload.setChapterFilePath(chapterFilePath);
        novelUpload.setUpdateTime(LocalDateTime.now());
        novelUploadMapper.updateById(novelUpload);

        log.info("数据库已更新，小说ID: {}, 章节数: {}", novelId, chapterCount);
    }
}
