package com.sequenceiq.mock;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sequenceiq.mock.verification.intercept.EndpointTestDecoratorInterceptor;

@Configuration
public class EndpointInterceptorConfig implements WebMvcConfigurer {

    @Inject
    private EndpointTestDecoratorInterceptor endpointTestDecoratorInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(endpointTestDecoratorInterceptor).addPathPatterns("/**");
    }

}
