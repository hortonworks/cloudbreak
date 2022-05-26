package com.sequenceiq.cloudbreak.auth.altus.config;


import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;

@Configuration
public class UmsChannelConfig {

    @Value("${altus.ums.host:}")
    private String endpoint;

    @Value("${altus.ums.port:8982}")
    private int port;

    @Bean
    public ManagedChannelWrapper umsManagedChannelWrapper() {
        return newManagedChannelWrapper(endpoint, port);
    }

    public static ManagedChannelWrapper newManagedChannelWrapper(String endpoint, int port) {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(endpoint, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }
}
