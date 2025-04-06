package org.example.product.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product2")
public class ProductController2 {

    @GetMapping("/sayHello")
    public String sayHello(@RequestParam("name") String name) {
        return "Hello, " + name;
    }
}
