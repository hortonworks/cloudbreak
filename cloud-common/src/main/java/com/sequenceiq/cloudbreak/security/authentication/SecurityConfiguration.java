package com.sequenceiq.cloudbreak.security.authentication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.client.CaasClient;

@Configuration
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cb.authentication.disabled", havingValue = "true")
    public AuthenticationService disabledAuthenticationService() {
        return new DisabledAuthenticationService();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService authenticationService(GrpcUmsClient grpcUmsClient, CaasClient caasClient) {
        if (grpcUmsClient.isUmsConfigured()) {
            return new UmsAuthenticationService(grpcUmsClient);
        } else {
            return new CaasAuthenticationService(caasClient);
        }
    }
}
