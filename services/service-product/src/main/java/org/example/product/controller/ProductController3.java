package org.example.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/product3")
@Slf4j
public class ProductController3 {

    @GetMapping("/sayHello")
    public String sayHello(@RequestParam("name") String name) {
        Date before = new Date();
        long duration = 1000 * 4;
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            log.error("", e);
        }
        Date after = new Date();
        log.info("before: {}, after: {} duration: {}ms", before, after, after.getTime() - before.getTime());
        return "Hello, " + name;
    }

}
