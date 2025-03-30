package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.Product;
import org.example.order.feign.ProductHttpsFeignClient1;
import org.example.order.feign.ProductHttpsFeignClient2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DirectFeignHttpsTest {

    @Autowired
    private ProductHttpsFeignClient1 productHttpsFeignClient1;
    @Autowired
    private ProductHttpsFeignClient2 productHttpsFeignClient2;

    @Test
    public void testGetProductHttps() {
        Long productId = 100L;
        Product product = productHttpsFeignClient1.getProduct(productId);
        log.info(String.format("city: %s, product: \n%s", productId, product));
    }

    @Test
    public void testGetProductHttps2() {
        Long productId = 100L;
        Product product = productHttpsFeignClient2.getProduct(productId);
        log.info(String.format("city: %s, product: \n%s", productId, product));
    }
}
