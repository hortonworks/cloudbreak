package com.sequenceiq.cloudbreak.sdx.common.grpc;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.cdp.servicediscovery.ServiceDiscoveryGrpc;
import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.sdx.common.model.ServiceDiscoveryChannelConfig;

import io.grpc.ManagedChannel;

public class ServiceDiscoveryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryClient.class);

    private final ManagedChannel channel;

    private final ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    ServiceDiscoveryClient(ManagedChannel channel, ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.serviceDiscoveryChannelConfig = serviceDiscoveryChannelConfig;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    private ServiceDiscoveryGrpc.ServiceDiscoveryBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return ServiceDiscoveryGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(serviceDiscoveryChannelConfig.getGrpcTimeoutSec()),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }

    public ServiceDiscoveryProto.ApiRemoteDataContext getRemoteDataContext(String sdxCrn) {
        ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request = ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest
                .newBuilder()
                .setDatalake(sdxCrn)
                .build();
        LOGGER.info("Created request to get RDC: {}. Crn: {}", request, sdxCrn);
        return newStub().describeDatalakeAsApiRemoteDataContext(request).getContext();
    }
}
