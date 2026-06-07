package com.qiniu.novel2script.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 剧本状态枚举
 */
@Getter
@AllArgsConstructor
public enum ScriptStatus {

    CONVERTING("CONVERTING", "转换中"),
    COMPLETED("COMPLETED", "转换完成"),
    FAILED("FAILED", "转换失败"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static ScriptStatus fromCode(String code) {
        for (ScriptStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ScriptStatus code: " + code);
    }
}
