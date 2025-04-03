package org.example.order.controller;

import org.example.order.feign.client.DirectFeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/direct-feign")
public class DirectFeignController {
    private final DirectFeignClient directFeignClient;

    public DirectFeignController(DirectFeignClient directFeignClient) {
        this.directFeignClient = directFeignClient;
    }

    @GetMapping("/query/tian")
    public String queryWeather(@RequestParam("city") String city) {
        return directFeignClient.queryWeather(city);
    }


}
