package com.sequenceiq.cloudbreak.security.authentication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@Configuration
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cb.authentication.disabled", havingValue = "true")
    public AuthenticationService disabledAuthenticationService() {
        return new DisabledAuthenticationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService authenticationService(GrpcUmsClient grpcUmsClient) {
        return new UmsAuthenticationService(grpcUmsClient);
    }
}
