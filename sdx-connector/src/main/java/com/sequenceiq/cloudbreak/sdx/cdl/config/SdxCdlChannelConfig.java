package com.sequenceiq.cloudbreak.sdx.cdl.config;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

@Configuration
public class SdxCdlChannelConfig {

    @Value("${cdl.sdx.host:localhost}")
    private String host;

    @Value("${cdl.sdx.port:8982}")
    private int port;

    @Bean(name = "sdxCdlManagedChannelWrapper")
    public ManagedChannelWrapper sdxCdlManagedChannelWrapper() {
        return newManagedChannelWrapper(host, port);
    }

    public ManagedChannelWrapper newManagedChannelWrapper(String host, int port) {
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
