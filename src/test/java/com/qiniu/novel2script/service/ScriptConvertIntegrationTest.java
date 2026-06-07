package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.*;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.enums.NovelStatus;
import com.qiniu.novel2script.exception.ScriptConvertException;
import com.qiniu.novel2script.mapper.NovelUploadMapper;
import com.qiniu.novel2script.vo.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("剧本转换集成测试")
class ScriptConvertIntegrationTest {

    @Autowired
    private ScriptConvertService scriptConvertService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private TextParserService textParserService;

    @Autowired
    private ChapterSplitterService chapterSplitterService;

    @Autowired
    private NovelUploadMapper novelUploadMapper;

    @Test
    @DisplayName("测试启动转换任务 - 小说不存在")
    void testStartConvertNovelNotFound() {
        // 尝试转换不存在的小说
        assertThrows(Exception.class, () -> {
            scriptConvertService.startConvert(999L);
        });
    }

    @Test
    @DisplayName("测试获取转换状态 - 任务不存在")
    void testGetConvertStatusNotFound() {
        // 尝试获取不存在的转换状态
        assertThrows(Exception.class, () -> {
            scriptConvertService.getConvertStatus(999L);
        });
    }

    @Test
    @DisplayName("测试取消转换任务 - 任务不存在")
    void testCancelConvertNotFound() {
        // 尝试取消不存在的转换任务
        assertThrows(Exception.class, () -> {
            scriptConvertService.cancelConvert(999L);
        });
    }

    @Test
    @DisplayName("测试重试转换任务 - 任务不存在")
    void testRetryConvertNotFound() {
        // 尝试重试不存在的转换任务
        assertThrows(Exception.class, () -> {
            scriptConvertService.retryConvert(999L);
        });
    }

    @Test
    @DisplayName("测试完整转换流程")
    void testFullConvertFlow() throws Exception {
        // 1. 创建测试小说数据
        NovelUpload novel = createTestNovel();

        // 2. 启动转换
        ConvertResult convertResult = scriptConvertService.startConvert(novel.getId());

        // 3. 验证转换结果
        assertNotNull(convertResult);
        assertNotNull(convertResult.getConvertId());
        assertEquals("转换中", convertResult.getStatus());

        // 4. 等待转换完成（异步）
        waitForCompletion(convertResult.getConvertId());

        // 5. 验证最终状态
        ConvertStatus status = scriptConvertService.getConvertStatus(convertResult.getConvertId());
        assertNotNull(status);
        assertEquals("转换完成", status.getStatus());
        assertEquals(100, status.getProgress());
    }

    @Test
    @DisplayName("测试取消转换任务")
    void testCancelConvert() throws Exception {
        // 1. 创建测试小说数据
        NovelUpload novel = createTestNovel();

        // 2. 启动转换
        ConvertResult convertResult = scriptConvertService.startConvert(novel.getId());

        // 3. 取消转换
        Result cancelResult = scriptConvertService.cancelConvert(convertResult.getConvertId());

        // 4. 验证取消结果
        assertNotNull(cancelResult);
        assertEquals(200, cancelResult.getCode());

        // 5. 验证状态已更新
        ConvertStatus status = scriptConvertService.getConvertStatus(convertResult.getConvertId());
        assertEquals("已取消", status.getStatus());
    }

    @Test
    @DisplayName("测试重复启动转换任务")
    void testDuplicateStartConvert() throws Exception {
        // 1. 创建测试小说数据
        NovelUpload novel = createTestNovel();

        // 2. 第一次启动转换
        scriptConvertService.startConvert(novel.getId());

        // 3. 第二次启动转换（应该抛出异常）
        assertThrows(ScriptConvertException.class, () -> {
            scriptConvertService.startConvert(novel.getId());
        });
    }

    private NovelUpload createTestNovel() {
        NovelUpload novel = NovelUpload.builder()
                .originalFilename("test-novel.txt")
                .storedFilename("test-novel-stored.txt")
                .filePath("src/test/resources/test-novels/test-3chapters.txt")
                .fileSize(1024L)
                .fileType("TXT")
                .chapterCount(3)
                .chapterFilePath("src/test/resources/test-novels/test-3chapters-chapters.json")
                .status(NovelStatus.PARSED)
                .uploadTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        novelUploadMapper.insert(novel);
        return novel;
    }

    private void waitForCompletion(Long convertId) throws InterruptedException {
        int maxWait = 300; // 最多等待300秒
        int waitTime = 0;

        while (waitTime < maxWait) {
            ConvertStatus status = scriptConvertService.getConvertStatus(convertId);
            if ("转换完成".equals(status.getStatus()) || "转换失败".equals(status.getStatus())) {
                return;
            }
            Thread.sleep(1000); // 等待1秒
            waitTime++;
        }

        fail("转换任务超时");
    }
}
