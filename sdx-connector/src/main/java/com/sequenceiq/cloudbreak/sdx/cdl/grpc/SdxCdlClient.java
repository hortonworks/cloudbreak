package com.sequenceiq.cloudbreak.sdx.cdl.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudGrpc;
import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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

    private CdlCrudGrpc.CdlCrudBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return CdlCrudGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()));
    }

    public CdlCrudProto.CreateDatalakeResponse createDatalake(String datalakeName, String environmentCrn, String type) {
        CdlCrudProto.CreateDatalakeRequest request = CdlCrudProto.CreateDatalakeRequest.newBuilder()
                .setDatalakeName(datalakeName)
                .setEnvironmentName(environmentCrn)
                .setDatabaseAvailabilityType(CdlCrudProto.DatabaseAvailabilityType.Value.valueOf(type.toUpperCase()))
                .build();
        return newStub().createDatalake(request);
    }

    public CdlCrudProto.DeleteDatalakeResponse deleteDatalake(String datalakeNameOrCrn, boolean force) {
        CdlCrudProto.DeleteDatalakeRequest request = CdlCrudProto.DeleteDatalakeRequest.newBuilder()
                .setDatalake(datalakeNameOrCrn)
                .setForce(force)
                .build();
        return newStub().deleteDatalake(request);
    }

    public CdlCrudProto.StartDatalakeResponse startDatalake(String datalakeNameOrCrn) {
        CdlCrudProto.StartDatalakeRequest request = CdlCrudProto.StartDatalakeRequest.newBuilder()
                .setDatalake(datalakeNameOrCrn)
                .build();
        return newStub().startDatalake(request);
    }

    public CdlCrudProto.StopDatalakeResponse stopDatalake(String datalakeNameOrCrn) {
        CdlCrudProto.StopDatalakeRequest request = CdlCrudProto.StopDatalakeRequest.newBuilder()
                .setDatalake(datalakeNameOrCrn)
                .build();
        return newStub().stopDatalake(request);
    }

    public CdlCrudProto.DatalakeResponse findDatalake(String environmentNameOrCrn, String datalakeNameOrCrn) {
        CdlCrudProto.FindDatalakeRequest request = CdlCrudProto.FindDatalakeRequest.newBuilder()
                .setEnvironment(environmentNameOrCrn)
                .setDatalake(datalakeNameOrCrn)
                .setAccountID(ThreadBasedUserCrnProvider.getAccountId())
                .build();
        return newStub().findDatalake(request);
    }

    public CdlCrudProto.DescribeDatalakeResponse describeDatalake(String datalakeNameOrCrn) {
        CdlCrudProto.DescribeDatalakeRequest request = CdlCrudProto.DescribeDatalakeRequest.newBuilder()
                .setDatalake(datalakeNameOrCrn)
                .setAccountID(ThreadBasedUserCrnProvider.getAccountId())
                .build();
        return newStub().describeDatalake(request);
    }

    public CdlCrudProto.DescribeServicesResponse describeDatalakeServices(String datalakeNameOrCrn) {
        CdlCrudProto.DescribeServicesRequest request = CdlCrudProto.DescribeServicesRequest.newBuilder()
                .setDatalake(datalakeNameOrCrn)
                .setAccountID(ThreadBasedUserCrnProvider.getAccountId())
                .build();
        return newStub().describeServices(request);
    }

    public CdlCrudProto.ListDatalakesResponse listDatalakes(String environmentNameOrCrn, String datalakeNameOrCrn) {
        CdlCrudProto.ListDatalakesRequest request = CdlCrudProto.ListDatalakesRequest.newBuilder()
                .setEnvironment(environmentNameOrCrn)
                .setDatalake(datalakeNameOrCrn)
                .setAccountID(ThreadBasedUserCrnProvider.getAccountId())
                .build();
        return newStub().listDatalakes(request);
    }

}
