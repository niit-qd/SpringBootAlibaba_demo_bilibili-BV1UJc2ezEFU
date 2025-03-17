package org.example.order.component;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
@Slf4j
public class NacosServiceListener implements ApplicationRunner {

    private final NacosConfigManager nacosConfigManager;

    public NacosServiceListener(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        String dataId = "service-order.yml";
        // String group = "DEFAULT_GROUP";
        String group = "order";
        Listener listener = new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info(configInfo);
            }
        };
        nacosConfigManager.getConfigService().addListener(dataId, group, listener);
    }
}
