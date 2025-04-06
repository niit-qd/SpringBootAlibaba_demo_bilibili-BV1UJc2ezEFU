package org.example.order.feign.client;

import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-product", contextId = "service-product-product4", url = "https://my.abc.com", path = "/product4", configuration = ProductFeignClient4.FeignClientConfiguration.class)
public interface ProductFeignClient4 {


    class FeignClientConfiguration {
        // @Bean
        // public Retryer retryer() {
        //     return new Retryer.Default(3000, 10, 4);
        // }

        @Bean
        public Retryer retryer() {
            return new MyRetryer();
        }

        @Slf4j
        static class MyRetryer implements Retryer {
            long interval;
            int index = 1;
            int count;

            public MyRetryer() {
                this.interval = 1000 * 2;
                this.count = 6;
            }

            public MyRetryer(long interval, int count) {
                this.interval = interval;
                this.index = count;
            }

            @Override
            public void continueOrPropagate(RetryableException e) {
                if (index >= count) {
                    log.info("try index = {}", index, e);
                    throw e;
                }
                log.info("try index = {}", index);
                index++;
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public Retryer clone() {
                Retryer retryer;
                try {
                    retryer = (Retryer) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
                return retryer;
            }
        }
    }

    // 验证 connectTimeout
    @GetMapping("/sayHello")
    String sayHi(@RequestParam("name") String name);

}
