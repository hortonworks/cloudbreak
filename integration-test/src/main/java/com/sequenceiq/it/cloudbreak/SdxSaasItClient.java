package com.sequenceiq.it.cloudbreak;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.saas.client.sdx.GrpcSdxSaasClient;
import com.sequenceiq.cloudbreak.saas.client.sdx.config.SdxSaasChannelConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

import io.opentracing.Tracer;

public class SdxSaasItClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcSdxSaasClient, Void, E, W> {

    private GrpcSdxSaasClient sdxSaasClient;

    SdxSaasItClient(String newId) {
        super(newId);
    }

    SdxSaasItClient() {
        this("SDX_SAAS_CLIENT");
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow checking is not supported by sdx saas client.");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service is not supported by sdx saas client");
    }

    @Override
    public GrpcSdxSaasClient getDefaultClient() {
        return sdxSaasClient;
    }

    public static synchronized SdxSaasItClient createProxySdxSaasClient(Tracer tracer, String host,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        SdxSaasItClient clientEntity = new SdxSaasItClient();
        SdxSaasChannelConfig sdxSaasChannelConfig = new SdxSaasChannelConfig();
        Field endpoint = ReflectionUtils.findField(SdxSaasChannelConfig.class, "endpoint");
        ReflectionUtils.makeAccessible(endpoint);
        ReflectionUtils.setField(endpoint, sdxSaasChannelConfig, host);
        Field port = ReflectionUtils.findField(SdxSaasChannelConfig.class, "port");
        ReflectionUtils.makeAccessible(port);
        ReflectionUtils.setField(port, sdxSaasChannelConfig, 8982);
        clientEntity.sdxSaasClient = GrpcSdxSaasClient.createClient(SdxSaasChannelConfig.newManagedChannelWrapper(host, 8982), sdxSaasChannelConfig, tracer);
        Field crnFactory = ReflectionUtils.findField(GrpcSdxSaasClient.class, "regionAwareInternalCrnGeneratorFactory");
        ReflectionUtils.makeAccessible(crnFactory);
        ReflectionUtils.setField(crnFactory, clientEntity.sdxSaasClient, regionAwareInternalCrnGeneratorFactory);
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(SdxSaasTestDto.class.getSimpleName());
    }
}
