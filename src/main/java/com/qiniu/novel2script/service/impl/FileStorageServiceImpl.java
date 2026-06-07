package com.qiniu.novel2script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiniu.novel2script.config.StorageProperties;
import com.qiniu.novel2script.dto.ChapterSplitResult;
import com.qiniu.novel2script.dto.ParseResult;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.enums.FileType;
import com.qiniu.novel2script.enums.NovelStatus;
import com.qiniu.novel2script.exception.FileStorageException;
import com.qiniu.novel2script.mapper.NovelUploadMapper;
import com.qiniu.novel2script.service.ChapterSplitterService;
import com.qiniu.novel2script.service.FileStorageService;
import com.qiniu.novel2script.service.TextParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final NovelUploadMapper novelUploadMapper;
    private final StorageProperties storageProperties;
    private final TextParserService textParserService;
    private final ChapterSplitterService chapterSplitterService;

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "docx");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public NovelUpload store(MultipartFile file) throws FileStorageException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        long fileSize = file.getSize();

        String storedFilename = generateStoredFilename(extension);
        String relativePath = generateRelativePath(storedFilename);
        Path absolutePath = storageProperties.getBasePath().resolve(relativePath);

        try {
            Files.createDirectories(absolutePath.getParent());
            Files.copy(file.getInputStream(), absolutePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件存储成功: {}", absolutePath);

            NovelUpload novelUpload = NovelUpload.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(relativePath)
                .fileSize(fileSize)
                .fileType(extension)
                .chapterCount(0)
                .status(NovelStatus.UPLOADED)
                .uploadTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

            novelUploadMapper.insert(novelUpload);
            log.info("文件元数据保存成功: id={}", novelUpload.getId());

            // 解析文件并分割章节
            try {
                FileType fileType = FileType.fromExtension(extension);
                ParseResult parseResult = textParserService.parse(absolutePath.toString(), fileType);
                
                ChapterSplitResult splitResult = chapterSplitterService.splitChapters(
                    novelUpload.getId(), parseResult.getCleanText());
                
                novelUpload.setChapterCount(splitResult.getChapterCount());
                novelUpload.setChapterFilePath(splitResult.getChapterFilePath());
                novelUpload.setStatus(NovelStatus.PARSED);
                novelUpload.setUpdateTime(LocalDateTime.now());
                novelUploadMapper.updateById(novelUpload);
                
                log.info("文件解析成功，章节数: {}", splitResult.getChapterCount());
            } catch (Exception e) {
                log.error("文件解析失败", e);
                // 清理已存储的文件和数据库记录
                try {
                    Files.deleteIfExists(absolutePath);
                } catch (IOException ioException) {
                    log.warn("清理文件失败: {}", absolutePath, ioException);
                }
                novelUploadMapper.deleteById(novelUpload.getId());
                throw e;
            }

            return novelUpload;

        } catch (IOException e) {
            log.error("文件存储失败: {}", e.getMessage(), e);
            throw new FileStorageException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long fileId) throws FileStorageException {
        NovelUpload novelUpload = novelUploadMapper.selectById(fileId);
        if (novelUpload == null) {
            throw new FileStorageException("文件不存在: " + fileId);
        }

        try {
            Path filePath = storageProperties.getBasePath().resolve(novelUpload.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("物理文件删除成功: {}", filePath);

            novelUploadMapper.deleteById(fileId);
            log.info("数据库记录删除成功: id={}", fileId);

        } catch (IOException e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new FileStorageException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public NovelUpload getFile(Long fileId) {
        NovelUpload novelUpload = novelUploadMapper.selectById(fileId);
        if (novelUpload == null) {
            throw new FileStorageException("文件不存在: " + fileId);
        }
        return novelUpload;
    }

    @Override
    public Page<NovelUpload> listFiles(int page, int size) {
        Page<NovelUpload> pageParam = new Page<>(page, size);
        QueryWrapper<NovelUpload> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("upload_time");
        return novelUploadMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public Path getFilePath(Long fileId) {
        NovelUpload novelUpload = getFile(fileId);
        return storageProperties.getBasePath().resolve(novelUpload.getFilePath());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileStorageException("文件名不能为空");
        }

        if (originalFilename.length() > 200) {
            throw new FileStorageException("文件名过长，请缩短文件名后再试");
        }

        String extension = getFileExtension(originalFilename);
        if (!SUPPORTED_TYPES.contains(extension.toLowerCase())) {
            throw new FileStorageException("不支持的文件类型: " + extension + "，仅支持: " + SUPPORTED_TYPES);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("文件大小超过限制，最大允许: 10MB");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String generateStoredFilename(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String generateRelativePath(String storedFilename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return datePath + "/" + storedFilename;
    }
}
