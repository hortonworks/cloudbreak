package com.sequenceiq.it.config

import org.springframework.beans.factory.config.PropertyResourceConfigurer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.testng.TestNG

import com.sequenceiq.it.SuiteContext

@Configuration
@ComponentScan("com.sequenceiq.it")
@EnableConfigurationProperties
class IntegrationTestConfiguration {

    @Bean
    fun suiteContext(): SuiteContext {
        return SuiteContext()
    }

    @Bean
    fun testNG(): TestNG {
        return TestNG()
    }

    companion object {
        @Bean
        fun propertySourcesPlaceholderConfigurer(): PropertyResourceConfigurer {
            return PropertySourcesPlaceholderConfigurer()
        }
    }
}
