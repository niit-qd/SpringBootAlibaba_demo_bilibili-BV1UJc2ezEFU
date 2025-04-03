package org.example.order.feign.client;

import org.example.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-product-https", path = "/product")
// @FeignClient(name = "https://service-product-https", path = "/product")
public interface ProductHttpsFeignClient2 {


    @GetMapping("/get/{id}")
    Product getProduct(@PathVariable("id") Long productId);

}
