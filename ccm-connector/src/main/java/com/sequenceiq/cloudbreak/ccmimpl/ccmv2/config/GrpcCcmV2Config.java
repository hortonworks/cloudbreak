package com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcCcmV2Config {

    @Value("${altus.ccmv2mgmt.host:thunderhead-clusterconnectivitymanagementv2.thunderhead-clusterconnectivitymanagementv2.svc.cluster.local}")
    private String host;

    @Value("${altus.ccmv2mgmt.port:80}")
    private int port;

    @Value("${altus.ccmv2mgmt.client.polling_interval_ms:5000}")
    private int pollingIntervalMs;

    @Value("${altus.ccmv2mgmt.client.timeout_ms:300000}")
    private int timeoutMs;

    @Value("${altus.ccmv2mgmt.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Bean
    public ManagedChannelWrapper ccmV2ManagedChannelWrapper() {
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

    public int getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public long getGrpcTimeoutSec() {
        return grpcTimeoutSec;
    }
}
