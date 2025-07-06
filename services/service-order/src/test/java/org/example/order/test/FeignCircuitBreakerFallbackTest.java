package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.order.feign.client.ProductFeignClient6;
import org.example.order.feign.client.ProductFeignClient7;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class FeignCircuitBreakerFallbackTest {

    @Autowired
    private ProductFeignClient6 productFeignClient6;

    @Autowired
    private ProductFeignClient7 productFeignClient7;

    @Test
    public void testFallbackForApi1() {
        String name = "ZhangSan";
        String result = productFeignClient6.api1(name);
        log.info("testFallbackForApi1. result = {}", result);
    }

    @Test
    public void testFallbackForApi2() {
        String name = "ZhangSan";
        String result = productFeignClient6.api2(name);
        log.info("testFallbackForApi2. result = {}", result);
    }

    @Test
    public void testFallbackForApi3() {
        String name = "ZhangSan";
        String result = productFeignClient6.api3(name);
        log.info("testFallbackForApi3. result = {}", result);
    }

    @Test
    public void testFallbackFactoryForApi1() {
        String name = "ZhangSan";
        String result = productFeignClient7.api1(name);
        log.info("testFallbackFactoryForApi1. result = {}", result);
    }

    @Test
    public void testFallbackFactoryForApi2() {
        String name = "ZhangSan";
        String result = productFeignClient7.api2(name);
        log.info("testFallbackFactoryForApi2. result = {}", result);
    }

    @Test
    public void testFallbackFactoryForApi3() {
        String name = "ZhangSan";
        String result = productFeignClient7.api3(name);
        log.info("testFallbackFactoryForApi3. result = {}", result);
    }
}
