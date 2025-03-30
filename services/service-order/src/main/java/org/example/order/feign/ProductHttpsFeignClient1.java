package org.example.order.feign;

import org.example.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-product-https-directly", url = "https://localhost:8015", path = "/product")
public interface ProductHttpsFeignClient1 {


    @GetMapping("/get/{id}")
    Product getProduct(@PathVariable("id") Long productId);

}
