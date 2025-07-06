package org.example.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

// 创建一个会导致异常的接口
@RestController
@RequestMapping("/product4")
@Slf4j
public class ProductController4 {

    // 超时
    @GetMapping("/api1")
    public String api1(@RequestParam("name") String name) {
        Date before = new Date();
        long duration = 1000 * 120;
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.error("", e);
        }
        Date after = new Date();
        log.info("before: {}, after: {} duration: {}ms", before, after, after.getTime() - before.getTime());
        return "Hello, " + name;
    }


    // 抛异常
    @GetMapping("/api2")
    public String api2(@RequestParam("name") String name) {
        throw new RuntimeException("Hello, " + name);
    }

}
