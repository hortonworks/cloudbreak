package com.sequenceiq.cloudbreak.sdx.cdl;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.kubedatalake.KubeDataLakeProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.cdl.config.SdxCdlChannelConfig;

@Service
public class GrpcSdxCdlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcSdxCdlClient.class);

    @Qualifier("sdxCdlManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private SdxCdlChannelConfig sdxCdlChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcSdxCdlClient createClient(ManagedChannelWrapper wrapper, SdxCdlChannelConfig config) {
        GrpcSdxCdlClient client = new GrpcSdxCdlClient();
        client.channelWrapper = Preconditions.checkNotNull(wrapper, "channelWrapper should not be null.");
        client.sdxCdlChannelConfig = Preconditions.checkNotNull(config, "sdxCdlChannelConfig should not be null");
        return client;
    }

    SdxCdlClient makeClient() {
        return new SdxCdlClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
    }

    public String createDatalake(String datalakeName, String environmentCrn, String type) {
        SdxCdlClient sdxCdlClient = makeClient();
        KubeDataLakeProto.CreateDatalakeResponse datalake = sdxCdlClient.createDatalake(datalakeName, environmentCrn, type);
        LOGGER.debug("CDL created: {}", datalake);
        return datalake.getCrn();
    }
}
