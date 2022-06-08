package com.sequenceiq.cloudbreak.saas.client.sdx;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.saas.client.sdx.config.ServiceDiscoveryChannelConfig;

import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

@Component
public class GrpcServiceDiscoveryClient {

    @Inject
    private ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private Tracer tracer;

    public static GrpcServiceDiscoveryClient createClient(ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig, Tracer tracer) {
        GrpcServiceDiscoveryClient client = new GrpcServiceDiscoveryClient();
        client.serviceDiscoveryChannelConfig = Preconditions.checkNotNull(serviceDiscoveryChannelConfig,
                "serviceDiscoveryChannelConfig should not be null.");
        client.tracer = Preconditions.checkNotNull(tracer, "tracer should not be null.");
        return client;
    }

    public String getRemoteDataContext(String sdxCrn) throws JsonProcessingException {
        ServiceDiscoveryClient serviceDiscoveryClient = makeClient();
        return JsonUtil.writeValueAsString(serviceDiscoveryClient.getRemoteDataContext(sdxCrn));
    }

    ServiceDiscoveryClient makeClient() {
        return new ServiceDiscoveryClient(makeWrapper().getChannel(), tracer, regionAwareInternalCrnGeneratorFactory);
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(serviceDiscoveryChannelConfig.getEndpoint(), serviceDiscoveryChannelConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }
}
