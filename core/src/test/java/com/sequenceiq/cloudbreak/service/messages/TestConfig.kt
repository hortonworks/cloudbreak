package com.sequenceiq.cloudbreak.service.messages

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {

    @Bean
    fun cloudbreakMessagesService(): CloudbreakMessagesService {
        return CloudbreakMessagesService()
    }

}
