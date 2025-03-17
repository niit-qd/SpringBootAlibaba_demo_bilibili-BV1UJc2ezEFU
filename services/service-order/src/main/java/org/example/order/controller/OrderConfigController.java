package org.example.order.controller;

import org.example.order.config.NacosOrderConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * a tester controller
 * <hr/>
 * 尽管在Nacos中更新配置后，SpringCloud应用会打印配置改变的内容。
 * 例如：
 * <pre>
 * 2025-03-16T12:04:19.855+08:00  INFO 13624 --- [service-order] [listener.task-0] c.a.n.client.config.impl.ClientWorker    : [fixed-127.0.0.1_8848] [data-received] dataId=service-order.yml, group=DEFAULT_GROUP, tenant=, md5=fafd5863b1eadf31e4f6c1ddb9b86f5d, content=order:
 *  timeout: 60min
 *  auto-confirm: 12d, type=yaml
 * </pre>
 * 但是并不会刷新`@Value(${key})`的值。
 * 需要使用启用`@RefreshScope`注解来刷新。
 * <br/>
 * 注意：`@RefreshScope`之影响被其注解的类。
 */
@RestController
@RequestMapping("/order-config")
@RefreshScope
public class OrderConfigController {

    @Value("${order.timeout}")
    private String timeOut;

    @Value("${order.auto-confirm}")
    private String autoConfirm;

    private final NacosOrderConfiguration nacosOrderConfiguration;

    public OrderConfigController(NacosOrderConfiguration nacosOrderConfiguration) {
        this.nacosOrderConfiguration = nacosOrderConfiguration;
    }

    @GetMapping("/get-config-timeout")
    public String getTimeOut() {
        return timeOut;
    }

    @GetMapping("/get-config-auto-confirm")
    public String getAutoConfirm() {
        return autoConfirm;
    }

    @GetMapping("/get-order-config")
    public String getOrderConfig() {
        return String.format("order-config: timeout = %s, auto-confirm = %s", timeOut, autoConfirm);
    }

    @GetMapping("/get-order-config2")
    public String getOrderConfig2() {
        return String.format("order-config: timeout = %s, auto-confirm = %s, desc = %s",
                nacosOrderConfiguration.getTimeout(), nacosOrderConfiguration.getAutoConfirm(), nacosOrderConfiguration.getDesc());
    }

}
