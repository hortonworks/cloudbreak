package com.sequenceiq.cloudbreak.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ConversionServiceFactoryBean;

@Configuration
@EnableSpringConfigured
public class ConversionConfig {

    @Bean(name = "conversionService")
    public ConversionServiceFactoryBean conversionServiceFactoryBean() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
        conversionServiceFactoryBean.afterPropertiesSet();
        return conversionServiceFactoryBean;
    }
}
