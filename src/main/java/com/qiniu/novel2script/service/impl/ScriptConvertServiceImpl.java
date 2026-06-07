package com.qiniu.novel2script.service.impl;

import com.qiniu.novel2script.ai.ScriptConverter;
import com.qiniu.novel2script.ai.SummaryGenerator;
import com.qiniu.novel2script.config.ScriptConvertProperties;
import com.qiniu.novel2script.config.StorageProperties;
import com.qiniu.novel2script.dto.*;
import com.qiniu.novel2script.dto.overview.NovelOverview;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.entity.ScriptOutput;
import com.qiniu.novel2script.enums.NovelStatus;
import com.qiniu.novel2script.enums.ScriptStatus;
import com.qiniu.novel2script.exception.ScriptConvertException;
import com.qiniu.novel2script.mapper.NovelUploadMapper;
import com.qiniu.novel2script.mapper.ScriptOutputMapper;
import com.qiniu.novel2script.service.ChapterSplitterService;
import com.qiniu.novel2script.service.NovelOverviewService;
import com.qiniu.novel2script.service.ScriptConvertService;
import com.qiniu.novel2script.service.YamlGeneratorService;
import com.qiniu.novel2script.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 剧本转换服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptConvertServiceImpl implements ScriptConvertService {

    private final ScriptConverter scriptConverter;
    private final SummaryGenerator summaryGenerator;
    private final NovelOverviewService novelOverviewService;
    private final YamlGeneratorService yamlGeneratorService;
    private final ChapterSplitterService chapterSplitterService;
    private final ScriptOutputMapper scriptOutputMapper;
    private final NovelUploadMapper novelUploadMapper;
    private final StorageProperties storageProperties;
    private final ScriptConvertProperties convertProperties;

    @Lazy
    @Autowired
    private ScriptConvertService self;

    // 摘要缓存
    private final Map<Integer, ChapterSummary> summaryCache = new HashMap<>();

    // 转换任务实时日志（convertId -> 日志队列）
    private final Map<Long, ConcurrentLinkedQueue<String>> convertLogs = new ConcurrentHashMap<>();

    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG_SIZE = 200;

    @Override
    @Transactional
    public ConvertResult startConvert(Long novelId) {
        log.info("启动转换任务，小说ID：{}", novelId);

        // 1. 查询小说信息，验证状态
        NovelUpload novel = novelUploadMapper.selectById(novelId);
        if (novel == null) {
            throw new ScriptConvertException("小说不存在，ID：" + novelId);
        }
        if (novel.getStatus() != NovelStatus.PARSED) {
            throw new ScriptConvertException("小说状态不正确，当前状态：" + novel.getStatus());
        }

        // 2. 检查是否已有进行中的转换任务
        ScriptOutput existingTask = scriptOutputMapper.selectByNovelIdAndStatus(novelId, ScriptStatus.CONVERTING);
        if (existingTask != null) {
            throw new ScriptConvertException("该小说已有进行中的转换任务，任务ID：" + existingTask.getId());
        }

        // 3. 加载章节数据
        List<Chapter> chapters = chapterSplitterService.loadChapters(novel.getChapterFilePath());
        if (chapters == null || chapters.isEmpty()) {
            throw new ScriptConvertException("章节数据为空");
        }

        // 4. 创建ScriptOutput记录
        ScriptOutput scriptOutput = ScriptOutput.builder()
                .novelId(novelId)
                .title(novel.getOriginalFilename())
                .status(ScriptStatus.CONVERTING)
                .progress(0)
                .currentChapter(0)
                .totalChapters(chapters.size())
                .totalScenes(0)
                .createdTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        scriptOutputMapper.insert(scriptOutput);

        // 5. 异步调用doConvert方法（通过代理调用以使@Async生效）
        self.doConvertAsync(scriptOutput.getId(), novelId, chapters);

        // 6. 返回ConvertResult
        return ConvertResult.builder()
                .convertId(scriptOutput.getId())
                .status(ScriptStatus.CONVERTING.getDescription())
                .message("转换任务已创建，开始转换...")
                .build();
    }

    @Override
    public ConvertStatus getConvertStatus(Long convertId) {
        ScriptOutput scriptOutput = scriptOutputMapper.selectById(convertId);
        if (scriptOutput == null) {
            throw new ScriptConvertException("转换任务不存在，ID：" + convertId);
        }

        return ConvertStatus.builder()
                .id(scriptOutput.getId())
                .status(scriptOutput.getStatus().name())
                .progress(scriptOutput.getProgress())
                .currentChapter(scriptOutput.getCurrentChapter())
                .totalChapters(scriptOutput.getTotalChapters())
                .message(getStatusMessage(scriptOutput))
                .errorMessage(scriptOutput.getErrorMessage())
                .build();
    }

    @Override
    @Transactional
    public Result cancelConvert(Long convertId) {
        ScriptOutput scriptOutput = scriptOutputMapper.selectById(convertId);
        if (scriptOutput == null) {
            throw new ScriptConvertException("转换任务不存在，ID：" + convertId);
        }
        if (scriptOutput.getStatus() != ScriptStatus.CONVERTING) {
            throw new ScriptConvertException("只能取消进行中的转换任务");
        }

        scriptOutputMapper.updateStatusWithError(convertId, ScriptStatus.CANCELLED, "用户取消");
        return Result.success("转换任务已取消");
    }

    @Override
    @Transactional
    public ConvertResult retryConvert(Long convertId) {
        ScriptOutput scriptOutput = scriptOutputMapper.selectById(convertId);
        if (scriptOutput == null) {
            throw new ScriptConvertException("转换任务不存在，ID：" + convertId);
        }
        if (scriptOutput.getStatus() != ScriptStatus.FAILED) {
            throw new ScriptConvertException("只能重试失败的转换任务");
        }

        // 重置状态
        scriptOutputMapper.updateStatus(convertId, ScriptStatus.CONVERTING);
        scriptOutputMapper.updateProgress(convertId, 0, 0);

        // 加载章节数据
        NovelUpload novel = novelUploadMapper.selectById(scriptOutput.getNovelId());
        List<Chapter> chapters = chapterSplitterService.loadChapters(novel.getChapterFilePath());

        // 异步重新转换（通过代理调用以使@Async生效）
        self.doConvertAsync(convertId, scriptOutput.getNovelId(), chapters);

        return ConvertResult.builder()
                .convertId(convertId)
                .status(ScriptStatus.CONVERTING.getDescription())
                .message("转换任务已重新启动")
                .build();
    }

    @Async("scriptConvertExecutor")
    public void doConvertAsync(Long convertId, Long novelId, List<Chapter> chapters) {
        convertLogs.put(convertId, new ConcurrentLinkedQueue<>());
        addLog(convertId, "转换任务开始执行");
        try {
            doConvert(convertId, novelId, chapters);
        } catch (Exception e) {
            log.error("转换任务执行失败，任务ID：{}", convertId, e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 2000) {
                errorMessage = errorMessage.substring(0, 2000);
            }
            addLog(convertId, "转换失败：" + errorMessage);
            scriptOutputMapper.updateStatusWithError(convertId, ScriptStatus.FAILED, errorMessage);
        }
    }

    private void doConvert(Long convertId, Long novelId, List<Chapter> chapters) {
        log.info("开始执行转换任务，任务ID：{}，章节数：{}", convertId, chapters.size());
        addLog(convertId, "共 " + chapters.size() + " 章，开始转换...");

        // 清理摘要缓存
        summaryCache.clear();

        // 1. 生成全书概览（10-20%）
        addLog(convertId, "正在生成全书概览...");
        updateProgress(convertId, 10, 0);
        NovelOverview overview = generateOverviewWithFallback(novelId, chapters);
        if (overview != null) {
            addLog(convertId, "全书概览生成完成");
        } else {
            addLog(convertId, "全书概览生成失败，将使用简单模式");
        }
        updateProgress(convertId, 20, 0);

        // 2. 逐章转换（20-90%）
        List<ChapterScript> chapterScripts = new ArrayList<>();
        String overviewStr = novelOverviewService.formatOverviewToString(overview);

        for (int i = 0; i < chapters.size(); i++) {
            // 检查任务是否被取消
            ScriptOutput task = scriptOutputMapper.selectById(convertId);
            if (task.getStatus() == ScriptStatus.CANCELLED) {
                log.info("转换任务已取消，任务ID：{}", convertId);
                addLog(convertId, "转换已取消");
                return;
            }

            Chapter currentChapter = chapters.get(i);

            // 构建上下文字符串（使用摘要设计）
            String contextStr = buildContextString(chapters, i);

            // 调用AI转换
            log.info("转换第{}章：{}", currentChapter.getChapterNumber(), currentChapter.getTitle());
            addLog(convertId, "正在转换第 " + currentChapter.getChapterNumber() + " 章：" + currentChapter.getTitle());
            ChapterScript chapterScript = scriptConverter.convertChapter(
                    overviewStr,
                    contextStr,
                    currentChapter.getTitle(),
                    currentChapter.getChapterNumber(),
                    currentChapter.getContent()
            );
            chapterScripts.add(chapterScript);
            addLog(convertId, "第 " + currentChapter.getChapterNumber() + " 章转换完成，"
                    + (chapterScript.getScenes() != null ? chapterScript.getScenes().size() : 0) + " 个场景");

            // 更新进度
            int progress = 20 + (int) (((i + 1.0) / chapters.size()) * 70);
            updateProgress(convertId, progress, i + 1);
        }

        // 3. 合并转换结果（90-95%）
        addLog(convertId, "正在合并转换结果...");
        updateProgress(convertId, 90, chapters.size());
        Map<String, Object> scriptData = mergeChapterScripts(chapterScripts);

        // 4. 生成YAML文件（95-100%）
        addLog(convertId, "正在生成YAML文件...");
        updateProgress(convertId, 95, chapters.size());
        Path yamlPath = storageProperties.getScriptPath().resolve("script_" + convertId + ".yaml");
        yamlGeneratorService.generateYaml(scriptData, yamlPath.toString());

        // 5. 更新任务状态为完成
        ScriptOutput scriptOutput = scriptOutputMapper.selectById(convertId);
        scriptOutput.setYamlFilePath(yamlPath.toString());
        scriptOutput.setStatus(ScriptStatus.COMPLETED);
        scriptOutput.setProgress(100);
        scriptOutput.setCurrentChapter(chapters.size());
        scriptOutput.setTotalScenes(countTotalScenes(chapterScripts));
        scriptOutput.setUpdateTime(LocalDateTime.now());
        scriptOutputMapper.updateById(scriptOutput);

        log.info("转换任务完成，任务ID：{}", convertId);
        addLog(convertId, "转换完成！共 " + countTotalScenes(chapterScripts) + " 个场景");
    }

    private NovelOverview generateOverviewWithFallback(Long novelId, List<Chapter> chapters) {
        try {
            return novelOverviewService.generateOverview(novelId, chapters);
        } catch (Exception e) {
            log.warn("全书概览生成失败，回退到简单模式", e);
            return null;
        }
    }

    private String buildContextString(List<Chapter> chapters, int currentIndex) {
        StringBuilder sb = new StringBuilder();

        // 1. 当前章节：使用原文
        Chapter currentChapter = chapters.get(currentIndex);
        sb.append("【当前章节】\n");
        sb.append(String.format("第%d章 %s\n%s\n\n",
                currentChapter.getChapterNumber(),
                currentChapter.getTitle(),
                currentChapter.getContent()));

        // 2. 前文章节：使用摘要
        int summaryWindowSize = convertProperties.getSummaryWindowSize();
        int start = Math.max(0, currentIndex - summaryWindowSize);

        for (int i = start; i < currentIndex; i++) {
            Chapter ch = chapters.get(i);
            sb.append("【前文摘要】\n");

            // 生成摘要（带缓存）
            ChapterSummary summary = getOrGenerateSummary(ch);
            if (summary != null) {
                sb.append(formatSummaryToString(summary));
            } else {
                // 摘要生成失败，使用原文
                sb.append(String.format("第%d章 %s\n%s\n\n",
                        ch.getChapterNumber(),
                        ch.getTitle(),
                        ch.getContent()));
            }
        }

        return sb.toString();
    }

    private ChapterSummary getOrGenerateSummary(Chapter chapter) {
        return summaryCache.computeIfAbsent(chapter.getChapterNumber(),
                k -> generateSummary(chapter));
    }

    private ChapterSummary generateSummary(Chapter chapter) {
        log.info("生成章节摘要：第{}章 {}", chapter.getChapterNumber(), chapter.getTitle());
        try {
            return summaryGenerator.generateSummary(
                    chapter.getChapterNumber(),
                    chapter.getTitle(),
                    chapter.getContent()
            );
        } catch (Exception e) {
            log.warn("章节摘要生成失败，使用原文：第{}章 {}", chapter.getChapterNumber(), chapter.getTitle(), e);
            return null;
        }
    }

    private String formatSummaryToString(ChapterSummary summary) {
        if (summary == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("第%d章 %s（摘要）\n", summary.getChapterNumber(), summary.getTitle()));

        if (summary.getCharacters() != null && !summary.getCharacters().isEmpty()) {
            sb.append("主要角色：").append(String.join("、", summary.getCharacters())).append("\n");
        }

        if (summary.getPlotSummary() != null) {
            sb.append("主要情节：").append(summary.getPlotSummary()).append("\n");
        }

        if (summary.getKeyScenes() != null && !summary.getKeyScenes().isEmpty()) {
            sb.append("关键场景：").append(String.join("；", summary.getKeyScenes())).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private Map<String, Object> mergeChapterScripts(List<ChapterScript> chapterScripts) {
        Map<String, Object> scriptData = new LinkedHashMap<>();

        // 元数据
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("title", chapterScripts.getFirst().getEpisodeTitle());
        metadata.put("total_episodes", chapterScripts.size());
        metadata.put("total_scenes", countTotalScenes(chapterScripts));
        metadata.put("created_at", LocalDateTime.now().toString());
        metadata.put("version", "1.0");
        scriptData.put("script", Map.of("metadata", metadata));

        // 合并角色表
        Map<String, com.qiniu.novel2script.dto.Character> allCharacters = new LinkedHashMap<>();
        for (ChapterScript cs : chapterScripts) {
            if (cs.getCharacters() != null) {
                for (com.qiniu.novel2script.dto.Character c : cs.getCharacters()) {
                    allCharacters.putIfAbsent(c.getName(), c);
                }
            }
        }
        scriptData.put("script", mergeIntoMap(scriptData.get("script"), "characters", new ArrayList<>(allCharacters.values())));

        // 合并场景
        List<Map<String, Object>> episodes = new ArrayList<>();
        int sceneNumber = 1;
        for (int i = 0; i < chapterScripts.size(); i++) {
            ChapterScript cs = chapterScripts.get(i);
            Map<String, Object> episode = new LinkedHashMap<>();
            episode.put("episode_number", i + 1);
            episode.put("title", cs.getEpisodeTitle());

            List<Map<String, Object>> scenes = new ArrayList<>();
            if (cs.getScenes() != null) {
                for (Scene s : cs.getScenes()) {
                    Map<String, Object> scene = new LinkedHashMap<>();
                    scene.put("scene_number", sceneNumber++);
                    scene.put("scene_header", s.getSceneHeader());
                    scene.put("content", s.getContent());
                    scenes.add(scene);
                }
            }
            episode.put("scenes", scenes);
            episodes.add(episode);
        }
        scriptData.put("script", mergeIntoMap(scriptData.get("script"), "episodes", episodes));

        return scriptData;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeIntoMap(Object mapObj, String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>((Map<String, Object>) mapObj);
        map.put(key, value);
        return map;
    }

    private int countTotalScenes(List<ChapterScript> chapterScripts) {
        return chapterScripts.stream()
                .filter(cs -> cs.getScenes() != null)
                .mapToInt(cs -> cs.getScenes().size())
                .sum();
    }

    private void updateProgress(Long convertId, int progress, int currentChapter) {
        scriptOutputMapper.updateProgress(convertId, progress, currentChapter);
    }

    private String getStatusMessage(ScriptOutput scriptOutput) {
        return switch (scriptOutput.getStatus()) {
            case CONVERTING -> String.format("正在转换，进度：%d%%，当前章节：%d/%d",
                    scriptOutput.getProgress(), scriptOutput.getCurrentChapter(), scriptOutput.getTotalChapters());
            case COMPLETED -> "转换完成";
            case FAILED -> "转换失败：" + scriptOutput.getErrorMessage();
            case CANCELLED -> "已取消";
        };
    }

    private void addLog(Long convertId, String message) {
        ConcurrentLinkedQueue<String> logs = convertLogs.get(convertId);
        if (logs != null) {
            String formatted = "[" + LocalDateTime.now().format(LOG_TIME_FMT) + "] " + message;
            logs.offer(formatted);
            // 限制日志数量，移除最早的
            while (logs.size() > MAX_LOG_SIZE) {
                logs.poll();
            }
        }
    }

    @Override
    public List<String> getConvertLogs(Long convertId) {
        ConcurrentLinkedQueue<String> logs = convertLogs.get(convertId);
        if (logs == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(logs);
    }
}
