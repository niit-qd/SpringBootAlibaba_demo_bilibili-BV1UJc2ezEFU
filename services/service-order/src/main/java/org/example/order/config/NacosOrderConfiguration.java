package org.example.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 代替{@link org.springframework.cloud.context.config.annotation.RefreshScope}。跟随属性变化更新。
 * <hr/>
 * <a href="https://blog.csdn.net/xgw1010/article/details/108548657">@RefreshScope与@ConfigurationProperties对比</a>
 */
@Configuration
@ConfigurationProperties(prefix = "order")
@Data
public class NacosOrderConfiguration {
    private String timeout;
    private String autoConfirm;
    private String desc;
}
