package com.qiniu.novel2script.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
/*
    vo：分页结果响应
    功能：对输入的小说内容进行分页，避免一次性把全部的大量数据进行读取，占用过多内存
 */
public class PageResult<T> {

    private List<T> content; //数据列表
    private long totalElements; //总记录数
    private int totalPages; //总页数
    private int pageNumber; //当前页数
    private int pageSize; //页大小
}
