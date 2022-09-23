package com.sequenceiq.cloudbreak.client;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

@Configuration
public class ClusterDnsConfig {

    @Value("${clusterdns.host:}")
    private String endpoint;

    @Value("${clusterdns.port:8982}")
    private int port;

    public String getEndpoint() {
        return endpoint;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(endpoint);
    }

    @Bean
    public ManagedChannelWrapper clusterDnsManagedChannelWrapper() {
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
