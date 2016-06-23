package com.sequenceiq.it.cloudbreak.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.it.cloudbreak.TemplateAdditionHelper

@Configuration
@EnableConfigurationProperties(ITProps::class)
class CloudbreakITConfig {
    @Bean internal fun templateAdditionHelper(): TemplateAdditionHelper {
        return TemplateAdditionHelper()
    }
}
