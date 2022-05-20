package com.sequenceiq.cloudbreak.saas.client.sdx;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.cdp.servicediscovery.ServiceDiscoveryGrpc;
import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

public class ServiceDiscoveryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryClient.class);

    private final ManagedChannel channel;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    ServiceDiscoveryClient(ManagedChannel channel, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    private ServiceDiscoveryGrpc.ServiceDiscoveryBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return ServiceDiscoveryGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }

    public ServiceDiscoveryProto.ApiRemoteDataContext getRemoteDataContext(String requestId, String sdxCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request = ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest
                .newBuilder()
                .setDatalake(sdxCrn)
                .build();
        return newStub(requestId).describeDatalakeAsApiRemoteDataContext(request).getContext();
    }
}
