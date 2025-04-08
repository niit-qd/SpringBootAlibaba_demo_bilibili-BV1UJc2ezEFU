package org.example.order.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.order.feign.client.ProductFeignClient3;
import org.example.order.feign.client.ProductFeignClient4;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/feign-err")
public class OrderFeignErrorController {

    private final ProductFeignClient3 productFeignClient3;
    private final ProductFeignClient4 productFeignClient4;

    public OrderFeignErrorController(ProductFeignClient3 productFeignClient3, ProductFeignClient4 productFeignClient4) {
        this.productFeignClient3 = productFeignClient3;
        this.productFeignClient4 = productFeignClient4;
    }

    // 不存在的接口
    public @GetMapping("/sayHi")
    String sayHi(@RequestParam("name") String name) {
        return productFeignClient3.sayHi(name);
    }

    // 验证超时问题：readTimeout
    // 当参数`name`未提供的时候，feign调用服务端返回缺失参数异常
    public @GetMapping("/sayHello")
    String sayHello(@RequestParam(name = "name", required = false) String name) {
        try {
            return productFeignClient3.sayHello(name);
        } catch (Exception e) {
            throw e;
        }
    }

    // 服务端不可访问
    @GetMapping("/sayHi2")
    public String sayHi2(@RequestParam("name") String name) {
        return productFeignClient4.sayHi(name);
    }

}
