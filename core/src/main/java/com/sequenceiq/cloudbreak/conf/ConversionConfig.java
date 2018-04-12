package com.sequenceiq.cloudbreak.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.context.support.ConversionServiceFactoryBean;

import com.sequenceiq.cloudbreak.converter.CloudbreakConversionServiceFactoryBean;

@Configuration
@EnableSpringConfigured
public class ConversionConfig {

    @Bean(name = "conversionService")
    public ConversionServiceFactoryBean conversionServiceFactoryBean() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new CloudbreakConversionServiceFactoryBean();
        conversionServiceFactoryBean.afterPropertiesSet();
        return conversionServiceFactoryBean;
    }
}
