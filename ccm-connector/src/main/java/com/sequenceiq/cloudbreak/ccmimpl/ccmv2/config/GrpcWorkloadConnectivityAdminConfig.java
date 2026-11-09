package com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcWorkloadConnectivityAdminConfig {

    @Value("${altus.jumpgateadmin.host:jumpgate-admin.jumpgate.svc.cluster.local}")
    private String host;

    @Value("${altus.jumpgateadmin.port:80}")
    private int port;

    @Value("${altus.jumpgateadmin.grpc.timeout.sec:10}")
    private long grpcTimeoutSec;

    @Bean
    public ManagedChannelWrapper workloadConnectivityAdminManagedChannelWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    public long getGrpcTimeoutSec() {
        return grpcTimeoutSec;
    }
}
