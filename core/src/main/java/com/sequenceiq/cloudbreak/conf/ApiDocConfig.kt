package com.sequenceiq.cloudbreak.conf

import java.io.IOException

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.cloudbreak.api.CoreApi
import com.sequenceiq.cloudbreak.util.FileReaderUtils

import io.swagger.jaxrs.config.BeanConfig

@Configuration
class ApiDocConfig {

    @Bean
    @Throws(IOException::class)
    fun swaggerBeanConfig(): BeanConfig {
        val beanConfig = BeanConfig()
        beanConfig.title = "Cloudbreak API"
        beanConfig.description = FileReaderUtils.readFileFromClasspath("swagger/cloudbreak-introduction")
        beanConfig.version = "1.4.0"
        beanConfig.schemes = arrayOf("http")
        beanConfig.basePath = CoreApi.API_ROOT_CONTEXT
        beanConfig.resourcePackage = "com.sequenceiq.cloudbreak.api"
        beanConfig.scan = true
        return beanConfig
    }
}
