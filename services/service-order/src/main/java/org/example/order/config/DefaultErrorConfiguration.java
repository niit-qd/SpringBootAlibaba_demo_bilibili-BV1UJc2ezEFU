package org.example.order.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Configuration
public class DefaultErrorConfiguration {

    @Bean
    @Profile(value = "dev")
    public DefaultErrorAttributes defaultErrorAttributes() {
        return new MyDefaultErrorAttributes();
    }

    static class MyDefaultErrorAttributes extends DefaultErrorAttributes {
        @Override
        public Map<String, Object> getErrorAttributes(WebRequest request, ErrorAttributeOptions options) {
            Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
            Throwable throwable = getError(request);
            if (null != throwable) {
                errorAttributes.put("detail", throwable.getMessage());
            }
            return errorAttributes;
        }
    }
}
