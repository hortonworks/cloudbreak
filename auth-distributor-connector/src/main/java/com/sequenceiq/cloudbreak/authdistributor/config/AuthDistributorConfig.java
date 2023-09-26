package com.sequenceiq.cloudbreak.authdistributor.config;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;

@Configuration
public class AuthDistributorConfig {

    @Value("${authdistributor.host:localhost}")
    private String endpoint;

    @Value("${authdistributor.port:8982}")
    private int port;

    @Value("${authdistributor.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Bean
    public ManagedChannelWrapper authDistributorManagedChannelWrapper() {
        return newManagedChannelWrapper(endpoint, port);
    }

    public static ManagedChannelWrapper newManagedChannelWrapper(String endpoint, int port) {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(endpoint, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    public long getGrpcTimeoutSec() {
        return grpcTimeoutSec;
    }
}