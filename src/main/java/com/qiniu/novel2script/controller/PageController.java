package com.qiniu.novel2script.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/upload")
    public String upload() {
        return "upload";
    }

    @GetMapping("/script")
    public String script() {
        return "script";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }
}
