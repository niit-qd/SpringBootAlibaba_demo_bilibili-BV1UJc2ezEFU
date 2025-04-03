package org.example.order.feign.client;

import feign.Logger;
import feign.slf4j.Slf4jLogger;
import org.example.bean.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// @RequestMapping annotation not allowed on @FeignClient interfaces
// 由于新版本的`spring-cloud-openfeign-core`在`SpringMvcContract#processAnnotationOnClass`中做了改动，导致注解`RequestMapping`不能与`FeignClient`作用在接口上。
// 尽管网上有方法给出了解决方案，但是考虑到`存在即有理`，还是不建议修改默认的锲约类。
// - [@RequestMapping和@FeginClient注解不能同时使用的问题](https://blog.csdn.net/qq_44734154/article/details/128624881)
// - [spring cloud openfeign中@RequestMapping和@FeginClient注解不能同时使用的问题](https://blog.csdn.net/zlpzlpzyd/article/details/132671988)

// 当前项目引用的是`spring-cloud-openfeign-core`，而`spring-cloud-openfeign-core`是`spring-cloud-openfeign`的子项目，版本号继承父项目。
// `spring-cloud-openfeign`
// 改动前`v2.2.9.RELEASE`：https://github.com/spring-cloud/spring-cloud-openfeign/blob/v2.2.9.RELEASE/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/support/SpringMvcContract.java#L184
// 改动后`v2.2.10.RELEASE`：https://github.com/spring-cloud/spring-cloud-openfeign/blob/v2.2.10.RELEASE/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/support/SpringMvcContract.java#L190

// 官方改动
// Commit d6783a6
// Block clas-level request mapping on Feign clients.
// https://github.com/spring-cloud/spring-cloud-openfeign/commit/d6783a6f1ec8dd08fafe76ecd072913d4e6f66b9
// v4.3.0-M2 ··· v2.2.10.RELEASE
// 如上，此问题是在提交d6783a6上发生的变动。首次引入此修改的版本是`v2.2.10.RELEASE`。
// 验证：由于在当前项目中基于springboot和springcloud的相关依赖的版本已经与变化很大，直接将`spring-cloud-openfeign-core`的版本改为`v2.2.10.RELEASE`会引入其它问题。

// 实际上，openfeign有自己的方案，只要在注解`FeignClient`中配置参数`path`即可，相当于使用注解`@RequestMapping`的参数`path`。
// path: Path prefix to be used by all method-level mappings.
// 添加该参数的提交是：
// spring-cloud-openfeign
// Commit d3cba4d
// Adds path parameter to @FeignClient.
// v4.3.0-M2 ··· v2.0.0.M1
// https://github.com/spring-cloud/spring-cloud-openfeign/commit/d3cba4d80ecfde246465b414c4618e452468f0c3

// @FeignClient(name = "service-product", path = "/product")
@FeignClient(name = "service-product", path = "/product", configuration = ProductFeignClient.ProductFeignClientConfiguration.class)
// @RequestMapping("/product")
public interface ProductFeignClient {

    static final class ProductFeignClientConfiguration<T> {
        @Bean(name = "feignLogger")
        Logger feignLogger() {
            return new Slf4jLogger(ProductFeignClient.class);
        }

        @Bean
        public Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }
    }

    @GetMapping("/get/{id}")
    Product getProduct(@PathVariable("id") Long productId);

}
