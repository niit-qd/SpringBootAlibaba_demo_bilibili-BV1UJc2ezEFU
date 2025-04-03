package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.Product;
import org.example.order.feign.client.DirectFeignClient;
import org.example.order.feign.client.ProductFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DirectFeignTest {

    @Autowired
    private DirectFeignClient directFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Test
    public void testQueryWeather() {
        String city = "北京";
        String weather = directFeignClient.queryWeather(city);
        log.info("city: {}, weather: \n{}", city, weather);
    }


    @Test
    public void testGetProduct() {
        Long productId = 100L;
        Product product = productFeignClient.getProduct(productId);
        log.info("city: {}, product: \n{}", productId, product);
    }
}
