package com.sequenceiq.periscope.config

import java.io.IOException

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.periscope.api.AutoscaleApi
import com.sequenceiq.periscope.utils.FileReaderUtils

import io.swagger.jaxrs.config.BeanConfig

@Configuration
class ApiDocConfig {

    @Bean
    @Throws(IOException::class)
    fun swaggerBeanConfig(): BeanConfig {
        val beanConfig = BeanConfig()
        beanConfig.title = "Auto-scaling API"
        beanConfig.description = FileReaderUtils.readFileFromClasspath("swagger/auto-scaling-introduction")
        beanConfig.version = "1.4.0"
        beanConfig.schemes = arrayOf("http")
        beanConfig.basePath = AutoscaleApi.API_ROOT_CONTEXT
        beanConfig.resourcePackage = "com.sequenceiq.periscope.api"
        beanConfig.scan = true
        return beanConfig
    }
}
