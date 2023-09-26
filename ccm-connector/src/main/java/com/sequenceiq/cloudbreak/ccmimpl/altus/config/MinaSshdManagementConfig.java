package com.sequenceiq.cloudbreak.ccmimpl.altus.config;


import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

@Configuration
public class MinaSshdManagementConfig {

    @Value("${altus.minasshdmgmt.host:thunderhead-clusterconnectivitymanagement.thunderhead-clusterconnectivitymanagement.svc.cluster.local}")
    private String host;

    @Value("${altus.minasshdmgmt.port:80}")
    private int port;

    @Value("${altus.minasshdmgmt.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Bean
    public ManagedChannelWrapper minaSshdManagedChannelWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getGrpcTimeoutSec() {
        return grpcTimeoutSec;
    }

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(host);
    }
}
