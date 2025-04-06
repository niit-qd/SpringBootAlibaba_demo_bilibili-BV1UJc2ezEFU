package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.order.feign.client.ProductFeignClient3;
import org.example.order.feign.client.ProductFeignClient4;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
@Slf4j
public class FeignTimeoutTest {

    @Autowired
    private ProductFeignClient3 productFeignClient3;
    @Autowired
    private ProductFeignClient4 productFeignClient4;

    @Test
    public void testFeignClientConnectTimeout() {
        Date before = new Date();
        log.info("testFeignClientConnectTimeout. before:{}", before);
        String name = "word";
        String result = null;
        try {
            result = productFeignClient4.sayHi(name);
        } catch (Exception e) {
            log.error("testFeignClientConnectTimeout", e);
        }
        Date after = new Date();
        log.info("testFeignClientConnectTimeout. name: {}, result: \n{}, before: {}, after: {}, duration: {}ms", name, result, before, after, before.getTime() - before.getTime());
    }

    @Test
    public void testFeignClientReadTimeout() {
        Date before = new Date();
        log.info("testFeignClientReadTimeout. before:{}", before);
        String name = "word";
        String result = null;
        try {
            result = productFeignClient3.sayHello(name);
        } catch (Exception e) {
            log.error("testFeignClientConnectTimeout", e);
        }
        Date after = new Date();
        log.info("testFeignClientReadTimeout. name: {}, result: \n{}, before: {}, after: {}, duration: {}ms", name, result, before, after, after.getTime() - before.getTime());
    }
}
