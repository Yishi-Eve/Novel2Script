package com.qiniu.novel2script.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniu.novel2script.ai.OverviewGenerator;
import com.qiniu.novel2script.config.StorageProperties;
import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.overview.NovelOverview;
import com.qiniu.novel2script.exception.ScriptConvertException;
import com.qiniu.novel2script.service.NovelOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全书概览服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelOverviewServiceImpl implements NovelOverviewService {

    private final OverviewGenerator overviewGenerator;
    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper;

    @Override
    public NovelOverview generateOverview(Long novelId, List<Chapter> chapters) {
        log.info("开始生成全书概览，小说ID：{}，章节数：{}", novelId, chapters.size());

        try {
            // 将章节列表序列化为字符串
            String chaptersText = formatChaptersToString(chapters);

            // 调用AI生成概览
            NovelOverview overview = overviewGenerator.generateOverview(chaptersText);

            // 保存概览到文件
            saveOverviewToFile(novelId, overview);

            log.info("全书概览生成成功，小说ID：{}", novelId);
            return overview;
        } catch (Exception e) {
            log.error("全书概览生成失败，小说ID：{}", novelId, e);
            throw new ScriptConvertException("全书概览生成失败", e);
        }
    }

    @Override
    public NovelOverview loadOverview(Long novelId) {
        Path overviewPath = getOverviewFilePath(novelId);
        if (!Files.exists(overviewPath)) {
            log.info("全书概览文件不存在，小说ID：{}", novelId);
            return null;
        }

        try {
            String content = Files.readString(overviewPath, StandardCharsets.UTF_8);
            NovelOverview overview = objectMapper.readValue(content, NovelOverview.class);
            log.info("加载全书概览成功，小说ID：{}", novelId);
            return overview;
        } catch (IOException e) {
            log.error("加载全书概览失败，小说ID：{}", novelId, e);
            return null;
        }
    }

    @Override
    public String formatOverviewToString(NovelOverview overview) {
        if (overview == null) {
            return "无全书概览信息";
        }

        StringBuilder sb = new StringBuilder();

        // 角色表
        if (overview.getCharacters() != null && !overview.getCharacters().isEmpty()) {
            sb.append("### 角色表\n");
            overview.getCharacters().forEach(c -> {
                sb.append("- ").append(c.getName())
                  .append("：").append(c.getDescription());
                if (c.getPersonality() != null) {
                    sb.append("，性格：").append(c.getPersonality());
                }
                if (c.getRelationships() != null) {
                    sb.append("，关系：").append(c.getRelationships());
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        // 情节线
        if (overview.getPlotLines() != null && !overview.getPlotLines().isEmpty()) {
            sb.append("### 情节线\n");
            overview.getPlotLines().forEach(p -> {
                sb.append("- 【").append(p.getType()).append("】")
                  .append(p.getDescription());
                if (p.getChapters() != null && !p.getChapters().isEmpty()) {
                    sb.append("（章节：").append(p.getChapters()).append("）");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        // 地点表
        if (overview.getLocations() != null && !overview.getLocations().isEmpty()) {
            sb.append("### 地点表\n");
            overview.getLocations().forEach(l -> {
                sb.append("- ").append(l.getName())
                  .append("：").append(l.getDescription());
                if (l.getChapters() != null && !l.getChapters().isEmpty()) {
                    sb.append("（章节：").append(l.getChapters()).append("）");
                }
                sb.append("\n");
            });
            sb.append("\n");
        }

        // 写作风格
        if (overview.getWritingStyle() != null) {
            sb.append("### 写作风格\n");
            if (overview.getWritingStyle().getLanguageStyle() != null) {
                sb.append("- 语言特点：").append(overview.getWritingStyle().getLanguageStyle()).append("\n");
            }
            if (overview.getWritingStyle().getNarrativeStyle() != null) {
                sb.append("- 叙事风格：").append(overview.getWritingStyle().getNarrativeStyle()).append("\n");
            }
            if (overview.getWritingStyle().getDialogueStyle() != null) {
                sb.append("- 对话特点：").append(overview.getWritingStyle().getDialogueStyle()).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatChaptersToString(List<Chapter> chapters) {
        return chapters.stream()
                .map(ch -> String.format("## 第%d章 %s\n\n%s", ch.getChapterNumber(), ch.getTitle(), ch.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private void saveOverviewToFile(Long novelId, NovelOverview overview) {
        try {
            Path overviewPath = getOverviewFilePath(novelId);
            Files.createDirectories(overviewPath.getParent());
            String json = objectMapper.writeValueAsString(overview);
            Files.writeString(overviewPath, json, StandardCharsets.UTF_8);
            log.info("全书概览保存成功：{}", overviewPath);
        } catch (IOException e) {
            log.error("全书概览保存失败，小说ID：{}", novelId, e);
        }
    }

    private Path getOverviewFilePath(Long novelId) {
        return storageProperties.getOverviewPath().resolve("novel_" + novelId + "_overview.json");
    }
}
