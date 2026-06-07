package com.qiniu.novel2script.service;

import com.qiniu.novel2script.dto.Chapter;
import com.qiniu.novel2script.dto.overview.NovelOverview;

import java.util.List;

/**
 * 全书概览服务接口
 */
public interface NovelOverviewService {

    /**
     * 生成全书概览
     *
     * @param novelId 小说ID
     * @param chapters 章节列表
     * @return 全书概览
     */
    NovelOverview generateOverview(Long novelId, List<Chapter> chapters);

    /**
     * 从文件加载全书概览
     *
     * @param novelId 小说ID
     * @return 全书概览，如果文件不存在则返回null
     */
    NovelOverview loadOverview(Long novelId);

    /**
     * 将全书概览格式化为字符串（用于提示词）
     *
     * @param overview 全书概览
     * @return 格式化后的字符串
     */
    String formatOverviewToString(NovelOverview overview);
}
