package com.sequenceiq.consumption.configuration.conversion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ConversionServiceFactoryBean;

@Configuration
@EnableSpringConfigured
public class ConversionConfiguration {

    @ConditionalOnMissingBean(ConversionServiceFactoryBean.class)
    public ConversionServiceFactoryBean conversionService() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new CloudbreakConversionServiceFactoryBean();
        conversionServiceFactoryBean.afterPropertiesSet();
        return conversionServiceFactoryBean;
    }
}
