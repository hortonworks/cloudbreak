package com.sequenceiq.cloudbreak.conf

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.aspectj.EnableSpringConfigured
import org.springframework.context.support.ConversionServiceFactoryBean

@Configuration
@EnableSpringConfigured
class ConversionConfig {

    @Bean(name = "conversionService")
    fun conversionServiceFactoryBean(): ConversionServiceFactoryBean {
        val conversionServiceFactoryBean = ConversionServiceFactoryBean()
        conversionServiceFactoryBean.afterPropertiesSet()
        return conversionServiceFactoryBean
    }
}
