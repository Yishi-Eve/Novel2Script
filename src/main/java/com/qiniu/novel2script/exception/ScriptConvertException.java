package com.qiniu.novel2script.exception;

/**
 * 剧本转换异常
 */
public class ScriptConvertException extends RuntimeException {

    public ScriptConvertException(String message) {
        super(message);
    }

    public ScriptConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
