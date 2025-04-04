package org.example.order.feign.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration(proxyBeanMethods = false)
public class MyFeignClientsConfiguration {
    @Bean
    Logger feignLogger() {
        // 这里只是示例，仅供学习用。现在基本都是使用SLF4J来作为log工具，所以在实际应用时，不建议使用。
        // 下面的步骤会设置logger的level为FINE，并将logger内容写入到指定的文件
        File dir = new File("target");
        if (dir.exists() && !dir.isDirectory()) {
            return null;
        }
        if (!dir.exists() && !dir.isDirectory()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Logger.JavaLogger logger = new Logger.JavaLogger();
        if (dir.isDirectory()) {
            logger.appendToFile(dir.getAbsolutePath() + File.separator + "JavaLogger.log");
        }
        return logger;
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
