package com.sequenceiq.cloudbreak.service.messages;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@Configuration
public class TestConfig {

    @Bean
    public CloudbreakMessagesService cloudbreakMessagesService() {
        return new CloudbreakMessagesService();
    }

}
