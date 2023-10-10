package com.sequenceiq.it.cloudbreak.microservice;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.saas.client.GrpcSdxSaasClient;
import com.sequenceiq.cloudbreak.sdx.saas.client.config.SdxSaasChannelConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public class SdxSaasItClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcSdxSaasClient, Void, E, W> {

    private GrpcSdxSaasClient sdxSaasClient;

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

    public static synchronized SdxSaasItClient createSdxSaasClient(String host,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        SdxSaasItClient clientEntity = new SdxSaasItClient();
        SdxSaasChannelConfig sdxSaasChannelConfig = new SdxSaasChannelConfig();
        Field endpoint = ReflectionUtils.findField(SdxSaasChannelConfig.class, "host");
        ReflectionUtils.makeAccessible(endpoint);
        ReflectionUtils.setField(endpoint, sdxSaasChannelConfig, host);
        Field port = ReflectionUtils.findField(SdxSaasChannelConfig.class, "port");
        ReflectionUtils.makeAccessible(port);
        ReflectionUtils.setField(port, sdxSaasChannelConfig, 8982);
        Field grpcTimeoutSec = ReflectionUtils.findField(SdxSaasChannelConfig.class, "grpcTimeoutSec");
        ReflectionUtils.makeAccessible(grpcTimeoutSec);
        ReflectionUtils.setField(grpcTimeoutSec, sdxSaasChannelConfig, 120);
        clientEntity.sdxSaasClient = GrpcSdxSaasClient.createClient(SdxSaasChannelConfig.newManagedChannelWrapper(host, 8982), sdxSaasChannelConfig);
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
