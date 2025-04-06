package org.example.order.feign.client;

import feign.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// @FeignClient(name = "service-product", contextId = "service-product-auth", path = "/auth")
@FeignClient(name = "service-product", contextId = "service-product-auth", path = "/auth", configuration = ProductFeignClient5.FeignClientConfiguration.class)
public interface ProductFeignClient5 {

    class FeignClientConfiguration {

        // FeignClientFactoryBean#configureUsingConfiguration并不会直接使用Collection<RequestInterceptor>形式的bean注册
        // 需要在中注册多个单独的RequestInterceptor类型的bean来实现。

        // @Bean
        // public Collection<RequestInterceptor> requestInterceptors() {
        //     Collection<RequestInterceptor> requestInterceptors = new ArrayList<RequestInterceptor>();
        //     requestInterceptors.add(new MyRequestInterceptor());
        //     return requestInterceptors;
        // }

        // 注册RequestInterceptor实例01
        @Bean
        public RequestInterceptor requestInterceptor1() {
            return new MyRequestInterceptor();
        }

        // 注册RequestInterceptor实例02
        @Bean
        public RequestInterceptor requestInterceptor2() {
            return new MyRequestInterceptor();
        }

        @Bean
        public ResponseInterceptor responseInterceptor() {
            return new MyResponseInterceptor();
        }
    }

    // 自定义请求拦截器
    @Slf4j
    class MyRequestInterceptor implements RequestInterceptor {

        // 添加一个实例跟踪变量，验证Collection<RequestInterceptor>的注册方式。
        private static int instanceCount;
        private int instanceIndex;

        public MyRequestInterceptor() {
            instanceCount++;
            instanceIndex = instanceCount;
            log.info("constructor. instance index = {}", instanceIndex);
        }

        private static final String[] AVAILABLE_TOKENS =
                {
                        // available
                        "hello",
                        "world",
                        // unavailable
                        "ZhangSan",
                        "Lisi",
                        "WangWu",
                        "ZhaoLiu",
                };

        @Override
        public void apply(RequestTemplate template) {
            int index = (int) (Math.random() * AVAILABLE_TOKENS.length);
            String account = AVAILABLE_TOKENS[index];

            // 添加header：Authorization
            String encoded1 = Base64.getEncoder().encodeToString(("Basic " + account).getBytes(StandardCharsets.UTF_8));
            template.removeHeader("Authorization"); // 先清空，否则自定义会叠加。
            template.header("Authorization", encoded1);
            // 添加header：X-token
            String encoded2 = Base64.getEncoder().encodeToString(account.getBytes(StandardCharsets.UTF_8));
            template.removeHeader("X-token"); // 先清空，否则自定义会叠加。
            template.header("X-token", encoded2);

            // 下面的header不会发生叠加
            template.header("Accept", "application/json");
            template.header("Content-Type", "application/json");
            template.header("Accept-Encoding", "gzip, deflate");
            template.header("Accept-Language", "en-US,en;q=0.8");
            template.header("Cache-Control", "no-cache");

            log.info("instance index = {}. account = {}, Authorization = {}, X-token = {}, template = {}", instanceIndex, account, encoded1, encoded2, template);
        }
    }

    // 自定义相应拦截器
    @Slf4j
    class MyResponseInterceptor implements ResponseInterceptor {

        @Override
        public Object intercept(InvocationContext invocationContext, Chain chain) throws Exception {
            Response response = invocationContext.response();
            log.info("response = {}", response);
            return chain.next(invocationContext);
        }
    }

    @PostMapping("queryWithAuthorization")
    String queryWithAuthorization(@RequestParam("name") String name);

    @PostMapping("queryWithXToken")
    String queryWithXToken(@RequestParam("name") String name);

}
