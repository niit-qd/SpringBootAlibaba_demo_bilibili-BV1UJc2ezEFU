package org.example.order.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

@SpringBootTest
@Slf4j
public class LoadBalanceTest {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Test
    public void testInstance() {
        for (int i = 0; i < 10; i++) {
            ServiceInstance serviceInstance = loadBalancerClient.choose("service-product");
            log.info("service-product:{}:{}", serviceInstance.getHost(), serviceInstance.getPort());
        }
    }
}
