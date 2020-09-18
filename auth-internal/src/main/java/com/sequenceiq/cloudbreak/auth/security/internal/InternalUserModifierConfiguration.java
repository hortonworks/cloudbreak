package com.sequenceiq.cloudbreak.auth.security.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalUserModifierConfiguration {

    @Bean
    @ConditionalOnMissingClass("com.sequenceiq.cloudbreak.service.user.UserService")
    public InternalUserModifier internalUserModifier() {
        return new InternalUserModifier();
    }
}
