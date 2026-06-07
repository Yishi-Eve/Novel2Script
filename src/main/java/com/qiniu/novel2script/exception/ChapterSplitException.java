package com.qiniu.novel2script.exception;

/**
 * 章节分割异常
 * 当章节分割过程中出现错误时抛出此异常
 */
public class ChapterSplitException extends RuntimeException {

    public ChapterSplitException(String message) {
        super(message);
    }

    public ChapterSplitException(String message, Throwable cause) {
        super(message, cause);
    }
}
