package com.sequenceiq.cloudbreak.sdx.saas.client;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.saas.client.config.SdxSaasChannelConfig;

@Component
public class GrpcSdxSaasClient {

    @Qualifier("sdxSaasManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private SdxSaasChannelConfig sdxSaasChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcSdxSaasClient createClient(ManagedChannelWrapper channelWrapper, SdxSaasChannelConfig sdxSaasChannelConfig) {
        GrpcSdxSaasClient client = new GrpcSdxSaasClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.sdxSaasChannelConfig = Preconditions.checkNotNull(sdxSaasChannelConfig, "sdxSaasChannelConfig should not be null.");
        return client;
    }

    public String createInstance(String sdxInstanceName, String environmentCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        return sdxSaasClient.createInstance(sdxInstanceName, environmentCrn,
                regionAwareInternalCrnGeneratorFactory.getRegion()).getCrn();
    }

    public void deleteInstance(String sdxInstanceCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        sdxSaasClient.deleteInstance(sdxInstanceCrn);
    }

    public Set<SDXSvcCommonProto.Instance> listInstances(String environmentCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        return sdxSaasClient.listInstances(environmentCrn);
    }

    SdxSaasClient makeClient() {
        return new SdxSaasClient(channelWrapper.getChannel(), sdxSaasChannelConfig, regionAwareInternalCrnGeneratorFactory);
    }
}
