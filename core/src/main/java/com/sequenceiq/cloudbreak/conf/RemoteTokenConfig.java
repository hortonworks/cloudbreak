package com.sequenceiq.cloudbreak.conf;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@Configuration
public class RemoteTokenConfig {

    @Value("${cb.client.id}")
    private String clientId;

    @Value("${cb.client.secret}")
    private String clientSecret;

    @Inject
    private GrpcUmsClient umsClient;
}
