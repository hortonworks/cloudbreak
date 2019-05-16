package com.sequenceiq.cloudbreak.authentication;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.caas.CaasClient;

@Configuration
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cb.authentication.disabled", havingValue = "true")
    public AuthenticationService disabledAuthenticationService() {
        return new DisabledAuthenticationService();
    }

    @Bean
    @ConditionalOnProperty("altus.ums.host")
    public AuthenticationService umsAuthenticationService(GrpcUmsClient grpcUmsClient) {
        return new UmsAuthenticationService(grpcUmsClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService caasAuthenticationService(CaasClient caasClient) {
        return new CaasAuthenticationService(caasClient);
    }
}
