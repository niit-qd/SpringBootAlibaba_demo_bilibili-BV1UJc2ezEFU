package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.Product;
import org.example.order.feign.client.ProductFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class FeignLoadBalanceTest {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Test
    public void testGetProduct() {
        Long productId = 100L;
        Product product = productFeignClient.getProduct(productId);
        log.info("city: {}, product: \n{}", productId, product);
    }
}
