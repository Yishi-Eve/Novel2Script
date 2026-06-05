package com.qiniu.novel2script.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.enums.NovelStatus;
import com.qiniu.novel2script.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NovelController.class)
class NovelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUploadNovel() throws Exception {
        // 准备
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "测试内容".getBytes()
        );

        NovelUpload mockResult = NovelUpload.builder()
            .id(1L)
            .originalFilename("test.txt")
            .storedFilename("uuid.txt")
            .filePath("2024/01/15/uuid.txt")
            .fileSize(12L)
            .fileType("txt")
            .chapterCount(0)
            .status(NovelStatus.UPLOADED)
            .uploadTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

        when(fileStorageService.store(any())).thenReturn(mockResult);

        // 执行和验证
        mockMvc.perform(multipart("/api/novel/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.originalFilename").value("test.txt"))
            .andExpect(jsonPath("$.data.fileType").value("txt"))
            .andExpect(jsonPath("$.data.status").value("已上传"));
    }

    @Test
    void testGetNovel() throws Exception {
        // 准备
        NovelUpload mockResult = NovelUpload.builder()
            .id(1L)
            .originalFilename("test.txt")
            .storedFilename("uuid.txt")
            .filePath("2024/01/15/uuid.txt")
            .fileSize(12L)
            .fileType("txt")
            .chapterCount(0)
            .status(NovelStatus.UPLOADED)
            .uploadTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

        when(fileStorageService.getFile(1L)).thenReturn(mockResult);

        // 执行和验证
        mockMvc.perform(get("/api/novel/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.originalFilename").value("test.txt"));
    }

    @Test
    void testDeleteNovel() throws Exception {
        // 执行和验证
        mockMvc.perform(delete("/api/novel/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
