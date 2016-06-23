package com.sequenceiq.cloudbreak.conf

import org.springframework.beans.factory.config.PropertyResourceConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

import reactor.Environment
import reactor.bus.EventBus


@Configuration
@ComponentScan
class TestConfig {

    @Bean
    fun env(): Environment {
        return Environment.initializeIfEmpty()
    }

    @Bean
    fun reactor(env: Environment): EventBus {
        return EventBus.create(env)
    }

    companion object {

        @Bean
        fun propertyResourceConfigurer(): PropertyResourceConfigurer {
            return PropertySourcesPlaceholderConfigurer()
        }
    }

}
