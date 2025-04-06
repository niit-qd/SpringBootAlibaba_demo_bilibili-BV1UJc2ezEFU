package org.example.order.feign.client;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@FeignClient(name = "service-product", contextId = "service-product-product3", path = "/product3", configuration = ProductFeignClient3.FeignClientConfiguration.class)
public interface ProductFeignClient3 {

    class FeignClientConfiguration {

        @Bean
        public Request.Options options() {
            return new Request.Options(4, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true);
        }

        // @Bean
        // public Retryer retryer() {
        //     return new Retryer.Default(3000, 10, 4);
        // }

        @Bean
        public Retryer retryer() {
            return new MyRetryer(1000 * 2, 1000 * 4, 2);
        }

        // 创建一个Retryer.Default的模拟类。目的是通过添加日志查看其执行过程。
        @Slf4j
        static class MyRetryer implements Retryer {

            private final int maxAttempts;
            private final long period;
            private final long maxPeriod;
            int attempt;
            long sleptForMillis;

            public MyRetryer() {
                this(100, SECONDS.toMillis(1), 5);
            }

            public MyRetryer(long period, long maxPeriod, int maxAttempts) {
                this.period = period;
                this.maxPeriod = maxPeriod;
                this.maxAttempts = maxAttempts;
                this.attempt = 1;
            }

            // visible for testing;
            protected long currentTimeMillis() {
                return System.currentTimeMillis();
            }

            public void continueOrPropagate(RetryableException e) {
                if (attempt >= maxAttempts) {
                    log.info("period = {}, attempt = {}", period, attempt, e);
                    System.err.printf("period = %s, attempt = %s%n%s\n", period, attempt, e);
                    throw e;
                }
                log.info("period = {}, attempt = {}", period, attempt);
                System.out.printf("period = %s, attempt = %s%n%s\n", period, attempt, e);
                attempt++;

                long interval;
                if (e.retryAfter() != null) {
                    interval = e.retryAfter() - currentTimeMillis();
                    if (interval > maxPeriod) {
                        interval = maxPeriod;
                    }
                    if (interval < 0) {
                        return;
                    }
                } else {
                    interval = nextMaxInterval();
                }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                sleptForMillis += interval;
            }

            /**
             * Calculates the time interval to a retry attempt.<br>
             * The interval increases exponentially with each attempt, at a rate of nextInterval *= 1.5
             * (where 1.5 is the backoff factor), to the maximum interval.
             *
             * @return time in milliseconds from now until the next attempt.
             */
            long nextMaxInterval() {
                long interval = (long) (period * Math.pow(1.5, attempt - 1));
                return Math.min(interval, maxPeriod);
            }

            @Override
            public Retryer clone() {
                return new MyRetryer(period, maxPeriod, maxAttempts);
            }
        }
    }

    // 不存在的接口
    @GetMapping("/sayHi")
    String sayHi(@RequestParam("name") String name);

    // 验证 readTimeout
    @GetMapping("/sayHello")
    String sayHello(@RequestParam("name") String name);

}
