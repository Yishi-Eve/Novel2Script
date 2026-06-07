package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.ConvertResult;
import com.qiniu.novel2script.dto.ConvertStatus;
import com.qiniu.novel2script.vo.Result;

import java.util.List;

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

    /**
     * 获取转换任务的实时日志
     *
     * @param convertId 转换任务ID
     * @return 日志消息列表
     */
    List<String> getConvertLogs(Long convertId);

    /**
     * 异步执行转换任务
     *
     * @param convertId 转换任务ID
     * @param novelId   小说ID
     * @param chapters  章节列表
     */
    void doConvertAsync(Long convertId, Long novelId, List<Chapter> chapters);
}
