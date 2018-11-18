package com.sequenceiq.caas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sequenceiq.caas.intercept.EndpointLoggingInterceptor;

@Configuration
public class EndpointLoggingInterceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new EndpointLoggingInterceptor()).addPathPatterns("/auth/*", "/oidc/*");
    }

}
