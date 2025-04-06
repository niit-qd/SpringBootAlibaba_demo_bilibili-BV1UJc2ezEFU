package org.example.order.feign.client;

import org.example.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 如果当前服务有多个Controller，可以考虑添加contextId进行区分。
// 为了避免创建Client时bean名称相同，个人习惯`contextId`的命名规则：`${name}-${path的变体}`。
@FeignClient(name = "service-product", contextId = "service-product-product2", path = "/product2")
public interface ProductFeignClient2 {

    @GetMapping("/get/{id}")
    Product getProduct(@PathVariable("id") Long productId);

}
