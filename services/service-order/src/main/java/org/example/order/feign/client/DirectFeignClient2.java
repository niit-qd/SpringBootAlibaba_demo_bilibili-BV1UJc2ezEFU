package org.example.order.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "direct", contextId = "52vmy-api", path = "/api")
public interface DirectFeignClient2 {

    @GetMapping("/wl/word")
    String queryWord(@RequestParam("word") String city);
}