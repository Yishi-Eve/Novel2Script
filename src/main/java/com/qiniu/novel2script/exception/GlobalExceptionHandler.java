package com.qiniu.novel2script.exception;

import com.qiniu.novel2script.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FileStorageException.class)
    public Result<Void> handleFileStorageException(FileStorageException e) {
        log.error("文件存储异常: {}", e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(FileParseException.class)
    public Result<Void> handleFileParseException(FileParseException e) {
        log.error("文件解析异常: {}", e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(500, "系统内部错误");
    }
}
