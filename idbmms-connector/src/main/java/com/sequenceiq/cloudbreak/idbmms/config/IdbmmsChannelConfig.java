package com.sequenceiq.cloudbreak.idbmms.config;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

/**
 * Configuration settings for the IDBroker Mapping Management Service endpoint.
 */
@Configuration
public class IdbmmsChannelConfig {

    @Value("${altus.idbmms.host:}")
    private String host;

    @Value("${altus.idbmms.port:8990}")
    private int port;

    @Bean
    public ManagedChannelWrapper idbmmsManagedChannelWrapper() {
        return newManagedChannelWrapper(host, port);
    }

    public static ManagedChannelWrapper newManagedChannelWrapper(String host, int port) {
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

    public boolean isConfigured() {
        return !StringUtil.isNullOrEmpty(host);
    }
}
