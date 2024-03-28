package com.sequenceiq.liftie.client;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

@Configuration
public class LiftieGrpcConfig {

    @Value("${liftie.grpc.host:localhost}")
    private String host;

    @Value("${liftie.grpc.port:8982}")
    private int port;

    @Value("${liftie.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Value("${altus.ums.caller:externalized-compute}")
    private String callingServiceName;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Bean("liftieManagedChannelWrapper")
    public ManagedChannelWrapper liftieManagedChannelWrapper() {
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

    public String getCallingServiceName() {
        return callingServiceName;
    }

    public String internalCrnForServiceAsString() {
        return regionAwareInternalCrnGeneratorFactory.externalizedCompute().getInternalCrnForServiceAsString();
    }
}
