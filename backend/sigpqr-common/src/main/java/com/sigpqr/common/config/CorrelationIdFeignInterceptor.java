package com.sigpqr.common.config;

import com.sigpqr.common.constants.AppConstants;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class CorrelationIdFeignInterceptor {

    @Bean
    public RequestInterceptor correlationIdRequestInterceptor() {
        return requestTemplate -> {
            String correlationId = MDC.get(AppConstants.CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                requestTemplate.header(AppConstants.CORRELATION_ID_HEADER, correlationId);
            }
        };
    }
}
