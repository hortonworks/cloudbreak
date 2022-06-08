package com.sequenceiq.cloudbreak.saas.client.sdx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminGrpc;
import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto;
import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.RequestIdUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

public class SdxSaasClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSaasClient.class);

    private final ManagedChannel channel;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    SdxSaasClient(ManagedChannel channel, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    private SDXSvcAdminGrpc.SDXSvcAdminBlockingStub newStub() {
        String requestId = RequestIdUtil.getOrGenerate(MDCUtils.getRequestId());
        return SDXSvcAdminGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }

    public SDXSvcCommonProto.Instance createInstance(String sdxInstanceName, String environmentCrn, String region) {
        SDXSvcAdminProto.CreateInstanceRequest request = SDXSvcAdminProto.CreateInstanceRequest.newBuilder()
                .setCloudPlatform(SDXSvcCommonProto.CloudPlatform.Value.AWS)
                .setCloudRegion(region)
                .setEnvironment(environmentCrn)
                .setName(sdxInstanceName)
                .build();
        return newStub().createInstance(request).getInstance();
    }

    public SDXSvcCommonProto.Instance deleteInstance(String sdxInstanceCrn) {
        SDXSvcAdminProto.DeleteInstanceRequest request = SDXSvcAdminProto.DeleteInstanceRequest.newBuilder()
                .setInstance(sdxInstanceCrn)
                .build();
        return newStub().deleteInstance(request).getInstance();
    }

    public Set<SDXSvcCommonProto.Instance> listInstances(String environmentCrn) {
        SDXSvcAdminProto.FindInstancesRequest.Builder requestBuilder = SDXSvcAdminProto.FindInstancesRequest.newBuilder()
                .setSearchByEnvironment(SDXSvcCommonProto.SearchByEnvironment.newBuilder()
                        .setEnvironment(environmentCrn)
                        .build());
        SDXSvcAdminGrpc.SDXSvcAdminBlockingStub stub = newStub();
        SDXSvcAdminProto.FindInstancesResponse findInstancesResponse;
        Set<SDXSvcCommonProto.Instance> instances = Sets.newHashSet();
        do {
            findInstancesResponse = stub.findInstances(requestBuilder.build());
            instances.addAll(findInstancesResponse.getInstancesList());
            requestBuilder.setPageToken(findInstancesResponse.getNextPageToken());
        } while (StringUtils.isNotEmpty(findInstancesResponse.getNextPageToken()));
        return instances;
    }
}
