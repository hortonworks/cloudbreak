package com.sequenceiq.cloudbreak.notification.client;

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
public class NotificationServiceConfig {

    @Value("${thunderheadnotification.service.host:localhost}")
    private String host;

    @Value("${thunderheadnotification.service.port:80}")
    private int port;

    @Value("${thunderheadnotification.service.grpc.timeout.sec:120}")
    private long grpcTimeoutSec;

    @Value("${thunderheadnotification.service.grpc.retry.number:3}")
    private int grpcRetryNumber;

    @Value("${altus.ums.caller:cloudbreak}")
    private String callingServiceName;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Bean
    public ManagedChannelWrapper notificationManagedChannelWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .enableRetry()
                        .maxRetryAttempts(grpcRetryNumber)
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

    public String internalCrnForIamServiceAsString() {
        return regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
    }
}
