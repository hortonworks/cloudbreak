package com.sequenceiq.cloudbreak.sdx.cdl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeGrpc;
import com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;

public class SdxCdlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCdlClient.class);

    private final ManagedChannel channel;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public SdxCdlClient(ManagedChannel channel, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = channel;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    private KubeDataLakeGrpc.KubeDataLakeBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return KubeDataLakeGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }

    public KubeDataLakeProto.CreateDatalakeResponse createDatalake(String datalakeName, String environmentCrn, String type) {
        KubeDataLakeProto.CreateDatalakeRequest request = KubeDataLakeProto.CreateDatalakeRequest.newBuilder()
                .setName(datalakeName)
                .setEnv(environmentCrn)
                .setType(KubeDataLakeProto.DatabaseAvailabilityType.Value.valueOf(type.toLowerCase()))
                .build();
        return newStub().createDatalake(request);
    }

}
