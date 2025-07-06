package org.example.order.feign.client;

import feign.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.TimeUnit;

// Feign Spring Cloud CircuitBreaker Fallbacks
@FeignClient(name = "service-product", contextId = "service-product-product4-2", path = "/product4",
        configuration = ProductFeignClient7.MyConfiguration.class,
        fallbackFactory = ProductFeignClient7.MyFallbackFactory.class)
public interface ProductFeignClient7 {

    // 超时
    @GetMapping("/api1")
    String api1(@RequestParam("name") String name);

    // 抛异常
    @GetMapping("/api2")
    String api2(@RequestParam("name") String name);

    // 抛异常
    @GetMapping("/api3")
    String api3(@RequestParam("name") String name);

    class MyConfiguration {
        @Bean
        public Request.Options options() {
            return new Request.Options(4, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
        }

        @Bean
        public MyFallbackFactory myFallbackFactory() {
            return new MyFallbackFactory();
        }
    }

    @Slf4j
//    @Component
    class MyFallbackFactory implements FallbackFactory<ProductFeignClient7.MyFallback> {
        @Override
        public MyFallback create(Throwable cause) {
            log.warn("create: ", cause);
            return new ProductFeignClient7.MyFallback();
        }
    }

    class MyFallback implements ProductFeignClient7 {
        @Override
        public String api1(String name) {
            return "default value for api1. name =" + name;
        }

        @Override
        public String api2(String name) {
            return "default value for api2. name =" + name;
        }

        @Override
        public String api3(String name) {
            return "default value for api3. name =" + name;
        }
    }
}