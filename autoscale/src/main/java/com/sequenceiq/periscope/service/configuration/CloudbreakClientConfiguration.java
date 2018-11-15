package com.sequenceiq.periscope.service.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.CloudbreakIdentityClient;
import com.sequenceiq.cloudbreak.client.CloudbreakIdentityClient.CloudbreakIdentityClientBuilder;

@Configuration
public class CloudbreakClientConfiguration {

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    @Value("${cb.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Autowired
    @Qualifier("identityServerUrl")
    private String identityServerUrl;

    @Value("${periscope.client.id}")
    private String clientId;

    @Value("${periscope.client.secret}")
    private String secret;

    @Bean
    public CloudbreakIdentityClient cloudbreakClient() {
        return new CloudbreakIdentityClientBuilder(cloudbreakUrl + cbRootContextPath, identityServerUrl, clientId).withSecret(secret).build();
    }
}
