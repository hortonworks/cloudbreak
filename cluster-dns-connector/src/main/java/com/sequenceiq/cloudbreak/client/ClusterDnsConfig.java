package com.sequenceiq.cloudbreak.client;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.netty.util.internal.StringUtil;

@Configuration
public class ClusterDnsConfig {

    private static final double GRPC_RETRIES_MAX_ATTEMPTS = 3.0;

    private static final double GRPC_RETRIES_BACKOFF_MULTIPLIER = 2.0;

    @Value("${clusterdns.host:}")
    private String host;

    @Value("${clusterdns.port:8982}")
    private int port;

    @Value("${clusterdns.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Bean
    public ManagedChannelWrapper clusterDnsManagedChannelWrapper() {

        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .defaultServiceConfig(createRetryServiceConfig())
                        .enableRetry()
                        .build());
    }

    private Map<String, Object> createRetryServiceConfig() {
        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", GRPC_RETRIES_MAX_ATTEMPTS);
        retryPolicy.put("initialBackoff", "1.0s");
        retryPolicy.put("maxBackoff", "8.0s");
        retryPolicy.put("backoffMultiplier", GRPC_RETRIES_BACKOFF_MULTIPLIER);
        retryPolicy.put("retryableStatusCodes", List.of(
                Status.Code.UNAVAILABLE.name(),
                Status.Code.DEADLINE_EXCEEDED.name(),
                Status.Code.UNKNOWN.name(),
                Status.Code.ABORTED.name(),
                Status.Code.INTERNAL.name()
        ));

        Map<String, Object> nameConfig = new HashMap<>();
        nameConfig.put("service", PublicEndpointManagementGrpc.SERVICE_NAME);

        Map<String, Object> methodConfig = new HashMap<>();
        methodConfig.put("name", List.of(nameConfig));
        methodConfig.put("retryPolicy", retryPolicy);
        methodConfig.put("timeout", grpcTimeoutSec + "s");
        methodConfig.put("waitForReady", true);

        return Map.of("methodConfig", List.of(methodConfig));
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
