package com.qiniu.novel2script.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.service.FileStorageService;
import com.qiniu.novel2script.vo.NovelUploadVO;
import com.qiniu.novel2script.vo.PageResult;
import com.qiniu.novel2script.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class NovelController {

    private final FileStorageService fileStorageService;

    @PostMapping("/novel/upload")
    public Result<NovelUploadVO> uploadNovel(@RequestParam("file") MultipartFile file) {
        log.info("收到文件上传请求: {}", file.getOriginalFilename());

        NovelUpload novelUpload = fileStorageService.store(file);
        NovelUploadVO vo = convertToVO(novelUpload);

        return Result.success(vo);
    }

    @GetMapping("/novel/{id}")
    public Result<NovelUploadVO> getNovel(@PathVariable Long id) {
        NovelUpload novelUpload = fileStorageService.getFile(id);
        NovelUploadVO vo = convertToVO(novelUpload);
        return Result.success(vo);
    }

    @GetMapping("/novels")
    public Result<PageResult<NovelUploadVO>> listNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<NovelUpload> novelPage = fileStorageService.listFiles(page, size);

        List<NovelUploadVO> voList = novelPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        PageResult<NovelUploadVO> pageResult = PageResult.<NovelUploadVO>builder()
            .content(voList)
            .totalElements(novelPage.getTotal())
            .totalPages((int) novelPage.getPages())
            .pageNumber((int) novelPage.getCurrent())
            .pageSize((int) novelPage.getSize())
            .build();

        return Result.success(pageResult);
    }

    @DeleteMapping("/novel/{id}")
    public Result<Void> deleteNovel(@PathVariable Long id) {
        fileStorageService.delete(id);
        return Result.success(null);
    }

    private NovelUploadVO convertToVO(NovelUpload entity) {
        return NovelUploadVO.builder()
            .id(entity.getId())
            .originalFilename(entity.getOriginalFilename())
            .storedFilename(entity.getStoredFilename())
            .fileType(entity.getFileType())
            .fileSize(entity.getFileSize())
            .fileSizeFormatted(formatFileSize(entity.getFileSize()))
            .status(entity.getStatus().getDescription())
            .uploadTime(entity.getUploadTime())
            .build();
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }
}
