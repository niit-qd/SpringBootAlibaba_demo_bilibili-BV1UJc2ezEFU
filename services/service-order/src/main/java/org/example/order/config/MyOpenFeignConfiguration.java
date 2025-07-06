package org.example.order.config;

import feign.Feign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class MyOpenFeignConfiguration {

    // @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return Feign.builder();
    }

    /**
     * 本例只是作为一个Demo，不要用于实际生产。
     *
     * @param <CONF>
     * @param <CONFB>
     * @return
     */
    // @Bean
    @ConditionalOnMissingBean
    public <CONF, CONFB extends ConfigBuilder<CONF>> CircuitBreakerFactory<CONF, CONFB> circuitBreakerFactory() {
        return new MyCircuitBreakerFactory<CONF, CONFB>();
    }

    @Slf4j
    static class MyCircuitBreakerFactory<CONF, CONFB extends ConfigBuilder<CONF>> extends CircuitBreakerFactory<CONF, CONFB> {
        @Override
        public CircuitBreaker create(String id) {
            return new CircuitBreaker() {
                @Override
                public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
                    log.info("CircuitBreakerFactory create");
                    return null;
                }
            };
        }

        @Override
        protected CONFB configBuilder(String id) {
            log.info("ConfigBuilder create");
            return null;
        }

        @Override
        public void configureDefault(Function defaultConfiguration) {
            log.info("configureDefault");
        }
    }
}