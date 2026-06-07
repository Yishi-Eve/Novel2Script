package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.ConvertResult;
import com.qiniu.novel2script.dto.ConvertStatus;
import com.qiniu.novel2script.vo.Result;

/**
 * 剧本转换服务接口
 */
public interface ScriptConvertService {

    /**
     * 启动转换任务
     *
     * @param novelId 小说ID
     * @return 转换结果
     */
    ConvertResult startConvert(Long novelId);

    /**
     * 获取转换状态
     *
     * @param convertId 转换任务ID
     * @return 转换状态
     */
    ConvertStatus getConvertStatus(Long convertId);

    /**
     * 取消转换任务
     *
     * @param convertId 转换任务ID
     * @return 操作结果
     */
    Result cancelConvert(Long convertId);

    /**
     * 重试转换任务
     *
     * @param convertId 转换任务ID
     * @return 转换结果
     */
    ConvertResult retryConvert(Long convertId);
}
