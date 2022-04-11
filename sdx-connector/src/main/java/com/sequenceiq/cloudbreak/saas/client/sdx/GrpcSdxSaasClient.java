package com.sequenceiq.cloudbreak.saas.client.sdx;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.sdxsvcadmin.SDXSvcAdminProto;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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

    public String createInstance(Optional<String> requestId, String sdxInstanceName, String environmentCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        String generatedRequestId = requestId.orElse(MDCBuilder.getOrGenerateRequestId());
        return sdxSaasClient.createInstance(generatedRequestId, sdxInstanceName, environmentCrn,
                regionAwareInternalCrnGeneratorFactory.getRegion()).getCrn();
    }

    public void deleteInstance(Optional<String> requestId, String sdxInstanceCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        String generatedRequestId = requestId.orElse(MDCBuilder.getOrGenerateRequestId());
        sdxSaasClient.deleteInstance(generatedRequestId, sdxInstanceCrn);
    }

    public Set<SDXSvcAdminProto.Instance> listInstances(Optional<String> requestId, String environmentCrn) {
        SdxSaasClient sdxSaasClient = makeClient();
        String generatedRequestId = requestId.orElse(MDCBuilder.getOrGenerateRequestId());
        return sdxSaasClient.listInstances(generatedRequestId, Crn.safeFromString(environmentCrn).getAccountId()).stream()
                .filter(instance -> instance.getEnvironmentsList().contains(environmentCrn))
                .collect(Collectors.toSet());
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
