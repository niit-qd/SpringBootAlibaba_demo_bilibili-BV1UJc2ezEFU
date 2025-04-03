package org.example.order.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.Order;
import org.example.bean.Product;
import org.example.order.feign.client.ProductFeignClient;
import org.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
    private final RestTemplate restTemplate;
    private final RestTemplate restTemplate2;
    private final ProductFeignClient productFeignClient;

    public OrderServiceImpl(DiscoveryClient discoveryClient, LoadBalancerClient loadBalancerClient, @Qualifier("restTemplate") RestTemplate restTemplate, @Qualifier("restTemplate2") RestTemplate restTemplate2, ProductFeignClient productFeignClient) {
        this.discoveryClient = discoveryClient;
        this.loadBalancerClient = loadBalancerClient;
        this.restTemplate = restTemplate;
        this.restTemplate2 = restTemplate2;
        this.productFeignClient = productFeignClient;
    }

    @Override
    public Order createOrder(Long productId, Long userId) {
//        Product product = getProductByIdFromRemote(productId);
//        Product product = getProductByIdFromRemoteWithLoadBalance(productId);
//        Product product = getProductByIdFromRemoteWithLoadBalanceAnnotation(productId);
        Product product = getProductByIdFromRemoteWithOpenFeign(productId);
        List<Product> products = Collections.singletonList(product);
        Order order = new Order();
        order.setId((long) (Math.random() * 1000) + 100);
        order.setTotalAmount(product.getPrice().multiply(new BigDecimal(product.getNum())));
        order.setUserId(userId);
        order.setNickName("anything you want");
        order.setProducts(products);
        return order;
    }

    private Product getProductByIdFromRemote(Long productId) {
        List<ServiceInstance> instances = discoveryClient.getInstances("service-product");
        String url = "http://" + instances.get(0).getHost() + ":" + instances.get(0).getPort() + "/product/get/" + productId;
        log.info("url: {} ", url);
        Product product = restTemplate.getForObject(url, Product.class);
        log.info("productId: {}, product: {}", productId, product);
        return product;
    }

    private Product getProductByIdFromRemoteWithLoadBalance(Long productId) {
        ServiceInstance instance = loadBalancerClient.choose("service-product");
        String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/product/get/" + productId;
        log.info("url: {} ", url);
        Product product = restTemplate.getForObject(url, Product.class);
        log.info("productId: {}, product: {}", productId, product);
        return product;
    }

    private Product getProductByIdFromRemoteWithLoadBalanceAnnotation(Long productId) {
        String url = "http://" + "service-product" + "/product/get/" + productId;
        log.info("url: {} ", url);
        Product product = restTemplate2.getForObject(url, Product.class);
        log.info("productId: {}, product: {}", productId, product);
        return product;
    }

    private Product getProductByIdFromRemoteWithOpenFeign(Long productId) {
        Product product = productFeignClient.getProduct(productId);
        log.info("productId: {}, product: {}", productId, product);
        return product;
    }

}
