package com.sequenceiq.cloudbreak.service.messages;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;

@TestConfiguration
public class TestConfig {

    @Bean
    public CloudbreakMessagesService cloudbreakMessagesService() {
        return new CloudbreakMessagesService();
    }

}
