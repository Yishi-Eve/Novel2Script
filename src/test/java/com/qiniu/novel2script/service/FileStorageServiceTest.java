package com.qiniu.novel2script.service;

import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.enums.NovelStatus;
import com.qiniu.novel2script.exception.FileStorageException;
import com.qiniu.novel2script.mapper.NovelUploadMapper;
import com.qiniu.novel2script.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private NovelUploadMapper novelUploadMapper;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "storageProperties", createStorageProperties());
    }

    @Test
    void testStoreTxtFile() throws IOException {
        // 准备
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "测试内容".getBytes()
        );

        NovelUpload mockUpload = NovelUpload.builder()
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

        when(novelUploadMapper.insert(any(NovelUpload.class))).thenReturn(1);

        // 执行
        NovelUpload result = fileStorageService.store(file);

        // 验证
        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalFilename());
        assertEquals("txt", result.getFileType());
        assertEquals(NovelStatus.UPLOADED, result.getStatus());

        verify(novelUploadMapper, times(1)).insert(any(NovelUpload.class));
    }

    @Test
    void testStoreEmptyFile() {
        // 准备
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );

        // 执行和验证
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.store(file);
        });

        verify(novelUploadMapper, never()).insert(any());
    }

    @Test
    void testStoreUnsupportedFileType() {
        // 准备
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "测试内容".getBytes()
        );

        // 执行和验证
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.store(file);
        });

        verify(novelUploadMapper, never()).insert(any());
    }

    @Test
    void testStoreNullFilename() {
        // 准备
        MockMultipartFile file = new MockMultipartFile(
            "file",
            null,
            "text/plain",
            "测试内容".getBytes()
        );

        // 执行和验证
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.store(file);
        });

        verify(novelUploadMapper, never()).insert(any());
    }

    @Test
    void testDeleteFile() {
        // 准备
        Long fileId = 1L;
        NovelUpload mockUpload = NovelUpload.builder()
            .id(fileId)
            .originalFilename("test.txt")
            .storedFilename("uuid.txt")
            .filePath("2024/01/15/uuid.txt")
            .fileSize(12L)
            .fileType("txt")
            .status(NovelStatus.UPLOADED)
            .uploadTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

        when(novelUploadMapper.selectById(fileId)).thenReturn(mockUpload);
        when(novelUploadMapper.deleteById(any(java.io.Serializable.class))).thenReturn(1);

        // 执行
        fileStorageService.delete(fileId);

        // 验证
        verify(novelUploadMapper, times(1)).selectById(fileId);
        verify(novelUploadMapper, times(1)).deleteById(any(java.io.Serializable.class));
    }

    @Test
    void testDeleteFileNotFound() {
        // 准备
        Long fileId = 999L;
        when(novelUploadMapper.selectById(fileId)).thenReturn(null);

        // 执行和验证
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.delete(fileId);
        });

        verify(novelUploadMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void testGetFile() {
        // 准备
        Long fileId = 1L;
        NovelUpload mockUpload = NovelUpload.builder()
            .id(fileId)
            .originalFilename("test.txt")
            .storedFilename("uuid.txt")
            .filePath("2024/01/15/uuid.txt")
            .fileSize(12L)
            .fileType("txt")
            .status(NovelStatus.UPLOADED)
            .uploadTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();

        when(novelUploadMapper.selectById(fileId)).thenReturn(mockUpload);

        // 执行
        NovelUpload result = fileStorageService.getFile(fileId);

        // 验证
        assertNotNull(result);
        assertEquals(fileId, result.getId());
        assertEquals("test.txt", result.getOriginalFilename());
    }

    @Test
    void testGetFileNotFound() {
        // 准备
        Long fileId = 999L;
        when(novelUploadMapper.selectById(fileId)).thenReturn(null);

        // 执行和验证
        assertThrows(FileStorageException.class, () -> {
            fileStorageService.getFile(fileId);
        });
    }

    private com.qiniu.novel2script.config.StorageProperties createStorageProperties() {
        com.qiniu.novel2script.config.StorageProperties properties = new com.qiniu.novel2script.config.StorageProperties();
        properties.setBasePath(tempDir);
        properties.setNovelPath(tempDir.resolve("novels"));
        properties.setScriptPath(tempDir.resolve("scripts"));
        return properties;
    }
}
