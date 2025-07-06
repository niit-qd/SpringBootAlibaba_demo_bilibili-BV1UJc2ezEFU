package org.example.order.feign.client;

import feign.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

// Feign Spring Cloud CircuitBreaker Fallbacks
@FeignClient(name = "service-product", contextId = "service-product-product-4-1", path = "/product4",
        configuration = ProductFeignClient6.MyConfiguration.class,
        fallback = ProductFeignClient6.MyFallback.class)
public interface ProductFeignClient6 {

    // 超时
    @GetMapping("/api1")
    String api1(@RequestParam("name") String name);

    // 抛异常
    @GetMapping("/api2")
    String api2(@RequestParam("name") String name);

    // 抛异常
    @GetMapping("/api3")
    String api3(@RequestParam("name") String name);

    @Slf4j
    class MyConfiguration {
        @Bean
        public Request.Options options() {
            return new Request.Options(4, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
        }

        @Bean
        public MyFallback myFallback() {
            return new MyFallback();
        }

        @Bean
        public CircuitBreakerFactory circuitBreakerFactory() {
            CircuitBreakerFactory breakerFactory = new MyCircuitBreakerFactory();
            log.info("CircuitBreakerFactory, breakerFactory: {}", breakerFactory);
            return breakerFactory;
        }
    }

    //    @Component
    class MyFallback implements ProductFeignClient6 {
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

    @Slf4j
    class MyCircuitBreakerFactory extends CircuitBreakerFactory {
        @Override
        public CircuitBreaker create(String id) {
            return new CircuitBreaker() {
                @Override
                public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
                    log.info("CircuitBreakerFactory create");
                    return null;
                }
            };
        }

        @Override
        protected ConfigBuilder configBuilder(String id) {
            log.info("ConfigBuilder create");
            return () -> null;
        }

        @Override
        public void configureDefault(Function defaultConfiguration) {
            log.info("configureDefault");
        }
    }

}
