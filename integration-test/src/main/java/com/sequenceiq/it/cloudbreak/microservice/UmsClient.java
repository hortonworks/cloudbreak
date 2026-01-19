package com.sequenceiq.it.cloudbreak.microservice;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsChannelConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public class UmsClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcUmsClient, Void, E, W> {

    private GrpcUmsClient umsClient;

    public UmsClient(String umsHost, int umsPort, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClientConfig clientConfig = new UmsClientConfig();
        Field callingServiceName = ReflectionUtils.findField(UmsClientConfig.class, "callingServiceName");
        ReflectionUtils.makeAccessible(callingServiceName);
        ReflectionUtils.setField(callingServiceName, clientConfig, "cloudbreak");
        Field grpcTimeoutSec = ReflectionUtils.findField(UmsClientConfig.class, "grpcTimeoutSec");
        ReflectionUtils.makeAccessible(grpcTimeoutSec);
        ReflectionUtils.setField(grpcTimeoutSec, clientConfig, 60L);
        umsClient = GrpcUmsClient.createClient(UmsChannelConfig.newManagedChannelWrapper(umsHost, umsPort), clientConfig,
                regionAwareInternalCrnGeneratorFactory);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        throw new TestFailException("Flow does not support by ums client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by ums client");
    }

    @Override
    public GrpcUmsClient getDefaultClient(TestContext testContext) {
        return umsClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(UmsTestDto.class.getSimpleName(),
                UmsGroupTestDto.class.getSimpleName());
    }
}
