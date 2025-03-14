package com.sequenceiq.cloudbreak.sdx.cdl.grpc;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

@Service
public class GrpcSdxCdlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcSdxCdlClient.class);

    @Qualifier("sdxCdlManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcSdxCdlClient createClient(ManagedChannelWrapper wrapper, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        GrpcSdxCdlClient client = new GrpcSdxCdlClient();
        client.channelWrapper = Preconditions.checkNotNull(wrapper, "channelWrapper should not be null.");
        client.regionAwareInternalCrnGeneratorFactory = Preconditions.checkNotNull(regionAwareInternalCrnGeneratorFactory,
                "regionAwareInternalCrnGeneratorFactory should not be null");
        return client;
    }

    SdxCdlClient makeClient() {
        return new SdxCdlClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
    }

    public String createDatalake(String datalakeName, String environmentCrn, String type) {
        SdxCdlClient sdxCdlClient = makeClient();
        CdlCrudProto.CreateDatalakeResponse datalake = sdxCdlClient.createDatalake(datalakeName, environmentCrn, type);
        LOGGER.debug("CDL created: {}", datalake);
        return datalake.getCrn();
    }

    public String deleteDatalake(String datalakeNameOrCrn, boolean force) {
        SdxCdlClient sdxCdlClient = makeClient();
        CdlCrudProto.DeleteDatalakeResponse response = sdxCdlClient.deleteDatalake(datalakeNameOrCrn, force);
        return response.getCrn();
    }

    public String startDatalake(String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        CdlCrudProto.StartDatalakeResponse response = sdxCdlClient.startDatalake(datalakeNameOrCrn);
        return response.getCrn();
    }

    public String stopDatalake(String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        CdlCrudProto.StopDatalakeResponse response = sdxCdlClient.stopDatalake(datalakeNameOrCrn);
        return response.getCrn();
    }

    public CdlCrudProto.DatalakeResponse findDatalake(String environmentNameOrCrn, String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        return sdxCdlClient.findDatalake(environmentNameOrCrn, datalakeNameOrCrn);
    }

    public CdlCrudProto.DescribeDatalakeResponse describeDatalake(String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        return sdxCdlClient.describeDatalake(datalakeNameOrCrn);
    }

    public CdlCrudProto.DescribeServicesResponse describeDatalakeServices(String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        return sdxCdlClient.describeDatalakeServices(datalakeNameOrCrn);
    }

    public CdlCrudProto.ListDatalakesResponse listDatalakes(String environmentNameOrCrn, String datalakeNameOrCrn) {
        SdxCdlClient sdxCdlClient = makeClient();
        return sdxCdlClient.listDatalakes(environmentNameOrCrn, datalakeNameOrCrn);
    }
}
