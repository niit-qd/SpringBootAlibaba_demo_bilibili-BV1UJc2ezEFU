package org.example.order.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 参考：
// - https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/FeignClient.java
// - https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/FeignClientBuilder.java
// - https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/FeignClientsRegistrar.java
// - https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/spring-cloud-openfeign-core/src/main/java/org/springframework/cloud/openfeign/FeignClientFactoryBean.java

// FeignClient参数说明：
// - name: he service id with optional protocol prefix.
//   服务id。
// - url: an absolute URL or resolvable hostname (the protocol is optional).
//   一个绝对URL或者可解析的域名（协议可不写）。
// - path: path prefix to be used by all method-level mappings.
//   可被所有方法层次映射使用的路径前缀

// url的构建
// FeignClientBuilder会调用FeignClientsRegistrar中的一些方法处理相关参数：
// 1. name: FeignClientsRegistrar.eagerlyRegisterFeignClientBeanDefinition
//    在这里，读取注解属性值。
// 2. url: FeignClientsRegistrar.getUrl(String url)
//    如果url非空，且url不是"#{xxx}"的形式：
//    - 如果url不包含“://”，即url没有自带协议前缀，则自动为其设置为“http”协议，设置其值为："http://" + url
// 3. url: FeignClientFactoryBean.getTarget()
//    FeignClientFactoryBean的name和url，来自于FeignClient注解中配置的对应属性。
//    - 如果url非空：并且不以"http://"和"https://"开头，则添加在url前追加"https//"
//      经验证，此种情况下，url已经在FeignClientsRegistrar.getUrl(String url)中追加过了，所以不会再执行此操作。
//    - 如果url为空：则直接使用FeignClientFactoryBean自身的name参数。




@FeignClient(name = "direct", url = "https://api.52vmy.cn", path = "/api")
public interface DirectFeignClient {

    @GetMapping("/query/tian")
    String queryWeather(@RequestParam("city") String city);
}


// 免费API
// - https://api.tcslw.cn/
// - https://api.52vmy.cn/