package org.example.product.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthorizationController {

    // 模拟一个可用账户列表
    private static final String[] AVAILABLE_TOKENS = {"Basic " + "hello", "Basic " + "world",};

    // 模拟一个可用的自定义token列表
    private static final String[] X_TOKENS = {"hello", "world",};

    @PostMapping("queryWithAuthorization")
    public String queryWithAuthorization(@RequestHeader("Authorization") String authorization, @RequestParam("name") String name) {
        log.info("queryWithAuthorization. authorization = {}", authorization);
        String auth = new String(Base64.getDecoder().decode(authorization.getBytes(StandardCharsets.UTF_8)));
        String result;
        if (Arrays.binarySearch(AVAILABLE_TOKENS, auth) >= 0) {
            result = "Hi, " + name + ", PASSED!";
        } else {
            result = "Hi, " + name + ", FAILED!";
        }
        log.info("queryWithAuthorization. Authorization = {}, name = {}, auth = {}, result = {}", authorization, name, auth, result);
        return result;
    }

    @PostMapping("queryWithXToken")
    public String queryWithXToken(@RequestHeader("X-Token") String xToken, @RequestParam("name") String name) {
        log.info("queryWithXToken. xToken = {}", xToken);
        String xt = new String(Base64.getDecoder().decode(xToken.getBytes(StandardCharsets.UTF_8)));
        String result;
        if (Arrays.binarySearch(X_TOKENS, xToken) >= 0) {
            result = "Hi, " + name + ", PASSED!";
        } else {
            result = "Hi, " + name + ", FAILED!";
        }
        log.info("queryWithXToken. xToken = {}, name = {}, token = {}, result = {}", xToken, name, xt, result);
        return result;
    }
}
