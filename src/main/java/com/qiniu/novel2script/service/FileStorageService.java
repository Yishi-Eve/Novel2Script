package com.qiniu.novel2script.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiniu.novel2script.entity.NovelUpload;
import com.qiniu.novel2script.exception.FileStorageException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    NovelUpload store(MultipartFile file) throws FileStorageException;

    void delete(Long fileId) throws FileStorageException;

    NovelUpload getFile(Long fileId);

    Page<NovelUpload> listFiles(int page, int size);

    Path getFilePath(Long fileId);
}
