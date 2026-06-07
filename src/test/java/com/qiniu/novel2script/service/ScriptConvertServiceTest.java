package com.qiniu.novel2script.service;

import com.qiniu.novel2script.ai.SummaryGenerator;
import com.qiniu.novel2script.config.ScriptConvertProperties;
import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.ChapterSummary;
import com.qiniu.novel2script.dto.overview.NovelOverview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("剧本转换服务测试")
class ScriptConvertServiceTest {

    @Autowired
    private ScriptConvertService scriptConvertService;

    @Autowired
    private SummaryGenerator summaryGenerator;

    @Autowired
    private NovelOverviewService novelOverviewService;

    @Autowired
    private ScriptConvertProperties convertProperties;

    @Test
    @DisplayName("测试摘要生成")
    void testGenerateSummary() {
        // 准备测试数据
        Chapter chapter = createTestChapter(1, "测试章节", "这是一个测试章节的内容，包含角色张三和李四的对话。");

        // 调用摘要生成
        ChapterSummary summary = summaryGenerator.generateSummary(
                chapter.getChapterNumber(),
                chapter.getTitle(),
                chapter.getContent()
        );

        // 验证结果
        assertNotNull(summary);
        assertNotNull(summary.getChapterNumber());
        assertNotNull(summary.getTitle());
        assertNotNull(summary.getCharacters());
        assertNotNull(summary.getPlotSummary());
    }

    @Test
    @DisplayName("测试摘要格式化")
    void testFormatSummaryToString() {
        // 准备测试数据
        ChapterSummary summary = ChapterSummary.builder()
                .chapterNumber(1)
                .title("测试章节")
                .characters(List.of("张三", "李四"))
                .plotSummary("张三和李四相遇")
                .keyScenes(List.of("客栈", "山路"))
                .keyDialogues(List.of("你好", "再见"))
                .build();

        // 调用格式化方法（通过反射或直接测试）
        // 这里我们验证ChapterSummary对象的创建
        assertNotNull(summary);
        assertEquals(1, summary.getChapterNumber());
        assertEquals("测试章节", summary.getTitle());
        assertEquals(2, summary.getCharacters().size());
    }

    @Test
    @DisplayName("测试全书概览生成")
    void testGenerateOverview() {
        // 准备测试数据
        List<Chapter> chapters = createTestChapters(3);

        // 调用全书概览生成
        NovelOverview overview = novelOverviewService.generateOverview(1L, chapters);

        // 验证结果
        assertNotNull(overview);
        assertNotNull(overview.getCharacters());
        assertNotNull(overview.getPlotLines());
        assertNotNull(overview.getLocations());
        assertNotNull(overview.getWritingStyle());
    }

    @Test
    @DisplayName("测试全书概览格式化")
    void testFormatOverviewToString() {
        // 准备测试数据
        NovelOverview overview = NovelOverview.builder()
                .characters(List.of())
                .plotLines(List.of())
                .locations(List.of())
                .writingStyle(null)
                .build();

        // 调用格式化方法
        String overviewStr = novelOverviewService.formatOverviewToString(overview);

        // 验证结果
        assertNotNull(overviewStr);
    }

    @Test
    @DisplayName("测试配置属性")
    void testConvertProperties() {
        // 验证配置属性
        assertNotNull(convertProperties);
        assertEquals(3, convertProperties.getSummaryWindowSize());
        assertEquals(10000, convertProperties.getOverviewMaxTokens());
        assertEquals(2000, convertProperties.getSummaryMaxTokens());
    }

    @Test
    @DisplayName("测试章节创建")
    void testChapterCreation() {
        // 准备测试数据
        Chapter chapter = createTestChapter(1, "第一章", "这是第一章的内容。");

        // 验证结果
        assertNotNull(chapter);
        assertEquals(1, chapter.getChapterNumber());
        assertEquals("第一章", chapter.getTitle());
        assertNotNull(chapter.getContent());
    }

    @Test
    @DisplayName("测试多章节创建")
    void testMultipleChaptersCreation() {
        // 准备测试数据
        List<Chapter> chapters = createTestChapters(5);

        // 验证结果
        assertNotNull(chapters);
        assertEquals(5, chapters.size());
        for (int i = 0; i < chapters.size(); i++) {
            assertEquals(i + 1, chapters.get(i).getChapterNumber());
        }
    }

    private Chapter createTestChapter(int chapterNumber, String title, String content) {
        return Chapter.builder()
                .chapterNumber(chapterNumber)
                .title(title)
                .content(content)
                .charCount(content.length())
                .lineCount(content.split("\n").length)
                .startPosition(0L)
                .endPosition((long) content.length())
                .build();
    }

    private List<Chapter> createTestChapters(int count) {
        List<Chapter> chapters = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String content = "这是第" + i + "章的内容。包含角色张三和李四的对话。";
            chapters.add(Chapter.builder()
                    .chapterNumber(i)
                    .title("第" + i + "章")
                    .content(content)
                    .charCount(content.length())
                    .lineCount(content.split("\n").length)
                    .startPosition(0L)
                    .endPosition((long) content.length())
                    .build());
        }
        return chapters;
    }
}
