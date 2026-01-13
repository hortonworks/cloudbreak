package com.sequenceiq.freeipa.flow.freeipa.common;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "freeipa.validation")
public class FreeIpaValidationProperties {

    private Set<String> failedMessages;

    public Set<String> getFailedMessages() {
        return failedMessages;
    }

    public void setFailedMessages(Set<String> failedMessages) {
        this.failedMessages = failedMessages;
    }
}
