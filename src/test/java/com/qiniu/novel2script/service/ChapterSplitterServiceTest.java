package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.ChapterSplitResult;
import com.qiniu.novel2script.dto.ChapterTitle;
import com.qiniu.novel2script.exception.ChapterSplitException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("章节分割服务测试")
class ChapterSplitterServiceTest {

    @Autowired
    private ChapterSplitterService chapterSplitterService;

    @Test
    @DisplayName("测试中文章节识别")
    void testChineseChapterDetection() {
        String text = """
                序言：这是一个关于江湖的故事。

                第一章 初入江湖

                    张三站在山顶，望着远处的村庄。这是他生活了十八年的地方，
                今天他就要离开这里，去闯荡江湖了。

                    "三儿，路上小心。"母亲站在门口，眼眶微红。

                    "娘，您放心，我会照顾好自己的。"张三背起行囊，头也不回
                地走了。他走了很久很久，终于来到了山脚下。回头望去，村庄已经
                变成了一个小点。他知道，从此以后，他就要开始新的生活了。

                第二章 奇遇

                    三个月后，张三来到了一座繁华的城市。这里人来人往，热闹非凡。
                张三从来没有见过这么多人，他感到既兴奋又紧张。他在街上闲逛，
                看着两边的店铺，心里想着该去哪里找个落脚的地方。

                    突然，他看到前面围了一群人，似乎在看什么热闹。张三挤进人群，
                发现是一个老者在卖艺。老者武艺高强，引得众人阵阵喝彩。

                第三章 危机

                    张三遇到了一个神秘的老人。老人告诉他，这座城市正面临着
                一场巨大的危机。原来，城外有一伙强盗，经常来骚扰百姓。

                    张三决定帮助百姓解决这个问题。他开始刻苦练武，准备迎接
                即将到来的挑战。每天清晨，他都会在城墙上练习剑法。

                第四章 转机

                    老人告诉张三一个秘密。原来，这伙强盗的首领是一个被朝廷
                通缉的逃犯。只要能抓住他，就能解决所有问题。

                    张三制定了一个详细的计划。他决定在月黑风高的夜晚动手。
                经过三天三夜的准备，一切就绪。

                第五章 结局

                    张三终于成为了大侠。他成功地抓住了强盗首领，保护了城市
                的安全。百姓们都非常感激他，纷纷送来礼物表示感谢。

                    但张三婉拒了所有礼物。他知道，真正的侠义不在于索取，
                而在于付出。他继续踏上了旅程，去帮助更多需要帮助的人。
                """;

        List<ChapterTitle> titles = chapterSplitterService.detectChapterTitles(text);

        assertNotNull(titles);
        assertTrue(titles.size() >= 5, "应检测到至少5个章节标题，实际: " + titles.size());
        assertEquals("第一章 初入江湖", titles.get(0).getTitle());
    }

    @Test
    @DisplayName("测试英文章节识别")
    void testEnglishChapterDetection() {
        String text = """
                Preface: This is a story about the world.

                Chapter 1 The Beginning

                    Zhang San stood on the mountaintop, looking at the village in the distance.
                This was the place where he had lived for eighteen years. Today he was leaving
                here to explore the world.

                    "Son, be careful on the road." His mother stood at the door, her eyes red.

                    "Don't worry, Mom. I'll take care of myself." Zhang San picked up his bag
                and walked away without looking back. He walked for a long time and finally
                reached the foot of the mountain.

                Chapter 2 The Adventure

                    Three months later, Zhang San arrived at a bustling city. There were so
                many people here, it was exciting and overwhelming. Zhang San had never seen
                so many people before.

                    He wandered through the streets, looking at the shops on both sides,
                wondering where he could find a place to stay.

                    Suddenly, he saw a crowd gathered ahead, watching something interesting.
                Zhang San squeezed into the crowd and found an old man performing martial arts.

                Chapter 3 The Crisis

                    Zhang San met a mysterious old man. The old man told him that the city
                was facing a huge crisis. There was a group of bandits outside the city
                who often harassed the people.

                    Zhang San decided to help the people solve this problem. He began to
                practice martial arts hard, preparing for the upcoming challenge.

                    Every morning, he would practice swordsmanship on the city wall.
                The old man taught him many useful techniques.
                """;

        List<ChapterTitle> titles = chapterSplitterService.detectChapterTitles(text);

        assertNotNull(titles);
        assertTrue(titles.size() >= 3, "应检测到至少3个章节标题，实际: " + titles.size());
        assertTrue(titles.get(0).getTitle().contains("Chapter 1"));
    }

    @Test
    @DisplayName("测试文本为空时抛出异常")
    void testSplitChaptersWithEmptyText() {
        assertThrows(ChapterSplitException.class, () -> {
            chapterSplitterService.splitChapters(1L, "");
        });
    }

    @Test
    @DisplayName("测试null文本时抛出异常")
    void testSplitChaptersWithNullText() {
        assertThrows(ChapterSplitException.class, () -> {
            chapterSplitterService.splitChapters(1L, null);
        });
    }

    @Test
    @DisplayName("测试目录过滤")
    void testDirectoryFiltering() {
        String text = """
                第一章 初入江湖 .................. 1
                第二章 奇遇 .................... 15
                第三章 危机 .................... 28
                第四章 转机 .................... 42
                第五章 结局 .................... 58

                第一章 初入江湖

                    张三站在山顶，望着远处的村庄。这是他生活了十八年的地方，
                今天他就要离开这里，去闯荡江湖了。

                    "三儿，路上小心。"母亲站在门口，眼眶微红。

                    "娘，您放心，我会照顾好自己的。"张三背起行囊，头也不回
                地走了。他走了很久很久，终于来到了山脚下。回头望去，村庄已经
                变成了一个小点。他知道，从此以后，他就要开始新的生活了。

                    张三沿着山路走了三天三夜，终于来到了一个小镇。镇上人来人往，
                热闹非凡。他找了一家客栈住下，准备休息一晚再继续赶路。

                第二章 奇遇

                    三个月后，张三来到了一座繁华的城市。这里人来人往，热闹非凡。
                张三从来没有见过这么多人，他感到既兴奋又紧张。他在街上闲逛，
                看着两边的店铺，心里想着该去哪里找个落脚的地方。

                    突然，他看到前面围了一群人，似乎在看什么热闹。张三挤进人群，
                发现是一个老者在卖艺。老者武艺高强，引得众人阵阵喝彩。

                    张三看得入迷，决定拜师学艺。他跪在老者面前，请求收他为徒。
                老者看他诚心，便答应了他的请求。从此，张三跟着老者学习武艺。

                第三章 危机

                    张三遇到了一个神秘的老人。老人告诉他，这座城市正面临着
                一场巨大的危机。原来，城外有一伙强盗，经常来骚扰百姓。

                    张三决定帮助百姓解决这个问题。他开始刻苦练武，准备迎接
                即将到来的挑战。每天清晨，他都会在城墙上练习剑法。

                    经过三个月的苦练，张三的武艺大有长进。他准备去找那伙强盗，
                为民除害。

                第四章 转机

                    老人告诉张三一个秘密。原来，这伙强盗的首领是一个被朝廷
                通缉的逃犯。只要能抓住他，就能解决所有问题。

                    张三制定了一个详细的计划。他决定在月黑风高的夜晚动手。
                经过三天三夜的准备，一切就绪。

                    月黑风高之夜，张三带着几个志同道合的朋友，悄悄潜入了
                强盗的山寨。经过一番激战，他们终于抓住了强盗首领。

                第五章 结局

                    张三终于成为了大侠。他成功地抓住了强盗首领，保护了城市
                的安全。百姓们都非常感激他，纷纷送来礼物表示感谢。

                    但张三婉拒了所有礼物。他知道，真正的侠义不在于索取，
                而在于付出。他继续踏上了旅程，去帮助更多需要帮助的人。

                    多年以后，张三成为了江湖上赫赫有名的大侠。他的故事
                被人们传颂，激励着一代又一代的年轻人。
                """;

        List<ChapterTitle> titles = chapterSplitterService.detectChapterTitles(text);

        // 应该过滤掉目录区域的标题
        assertNotNull(titles);
        assertTrue(titles.size() >= 5, "应检测到至少5个章节标题，实际: " + titles.size());
        // 验证目录被过滤，第一个标题应该是"第一章 初入江湖"而不是目录中的标题
        assertEquals("第一章 初入江湖", titles.get(0).getTitle());
    }
}
