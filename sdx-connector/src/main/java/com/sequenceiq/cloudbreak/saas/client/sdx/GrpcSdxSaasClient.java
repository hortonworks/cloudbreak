package com.sequenceiq.cloudbreak.saas.client.sdx;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.sdxsvccommon.SDXSvcCommonProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.saas.client.sdx.config.SdxSaasChannelConfig;

import io.grpc.ManagedChannelBuilder;
import io.opentracing.Tracer;

@Component
public class GrpcSdxSaasClient {

    @Inject
    private SdxSaasChannelConfig sdxSaasChannelConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private Tracer tracer;

    public static GrpcSdxSaasClient createClient(SdxSaasChannelConfig sdxSaasChannelConfig, Tracer tracer) {
        GrpcSdxSaasClient client = new GrpcSdxSaasClient();
        client.sdxSaasChannelConfig = Preconditions.checkNotNull(sdxSaasChannelConfig, "sdxSaasChannelConfig should not be null.");
        client.tracer = Preconditions.checkNotNull(tracer, "tracer should not be null.");
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
        return new SdxSaasClient(makeWrapper().getChannel(), tracer, regionAwareInternalCrnGeneratorFactory);
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(sdxSaasChannelConfig.getEndpoint(), sdxSaasChannelConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }
}
