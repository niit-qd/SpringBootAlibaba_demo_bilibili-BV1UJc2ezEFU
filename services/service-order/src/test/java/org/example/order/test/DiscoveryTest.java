package org.example.order.test;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
public class DiscoveryTest {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryTest.class);
    private static final Logger log = LoggerFactory.getLogger(DiscoveryTest.class);

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private NacosDiscoveryClient nacosDiscoveryClient;


    @Test
    public void testDiscoveryClient() {
        discoveryClient.getServices().forEach(serviceId -> {
            log.info("=====================================");
            log.info("serviceId: {}", serviceId);
            discoveryClient.getInstances(serviceId).forEach(instance -> {
                log.info("serviceId: {}, instance, host: {}, port: {}", serviceId, instance.getHost(), instance.getPort());
            });
            log.info("=====================================");
        });
    }

    @Test
    public void testNacosDiscoveryClient() {
        nacosDiscoveryClient.getServices().forEach(serviceId -> {
            log.info("=====================================");
            log.info("serviceId: {}", serviceId);
            nacosDiscoveryClient.getInstances(serviceId).forEach(instance -> {
                log.info("serviceId: {}, instance, host: {}, port: {}", serviceId, instance.getHost(), instance.getPort());
            });
            log.info("=====================================");
        });
    }
}
