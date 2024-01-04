package com.sequenceiq.cloudbreak.sdx.saas.client;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.saas.client.config.ServiceDiscoveryChannelConfig;

@Component
public class GrpcServiceDiscoveryClient {

    @Qualifier("discoveryManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcServiceDiscoveryClient createClient(ManagedChannelWrapper channelWrapper, ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig) {
        GrpcServiceDiscoveryClient client = new GrpcServiceDiscoveryClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.serviceDiscoveryChannelConfig = Preconditions.checkNotNull(serviceDiscoveryChannelConfig,
                "serviceDiscoveryChannelConfig should not be null.");
        return client;
    }

    public String getRemoteDataContext(String sdxCrn) throws JsonProcessingException {
        ServiceDiscoveryClient serviceDiscoveryClient = makeClient();
        return JsonUtil.writeValueAsString(serviceDiscoveryClient.getRemoteDataContext(sdxCrn));
    }

    ServiceDiscoveryClient makeClient() {
        return new ServiceDiscoveryClient(channelWrapper.getChannel(), serviceDiscoveryChannelConfig, regionAwareInternalCrnGeneratorFactory);
    }
}
