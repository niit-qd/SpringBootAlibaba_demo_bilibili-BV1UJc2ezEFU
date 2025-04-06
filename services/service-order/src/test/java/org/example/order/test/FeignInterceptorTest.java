package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.order.feign.client.ProductFeignClient5;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class FeignInterceptorTest {

    private static final String[] AVAILABLE_ACCOUNTS =
            {
                    "ZhangSan",
                    "Lisi",
                    "WangWu",
                    "ZhaoLiu",
            };

    @Autowired
    private ProductFeignClient5 productFeignClient5;

    @Test
    public void testQueryWithAuthorization() {
        int index = (int) (Math.random() * AVAILABLE_ACCOUNTS.length);
        String account = AVAILABLE_ACCOUNTS[index];
        String response = null;
        try {
            response = productFeignClient5.queryWithAuthorization(account);
        } catch (Exception e) {
            log.error("", e);
        }
        log.info("account = {}, response = {}", account, response);
    }

    @Test
    public void testQueryWithXToken() {
        int index = (int) (Math.random() * AVAILABLE_ACCOUNTS.length);
        String account = AVAILABLE_ACCOUNTS[index];
        String response = null;
        try {
            response = productFeignClient5.queryWithXToken(account);
        } catch (Exception e) {
            log.error("", e);
        }
        log.info("account = {}, response = {}", account, response);
    }
}
