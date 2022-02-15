package com.sequenceiq.it.cloudbreak;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsChannelConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

import io.opentracing.Tracer;

public class UmsClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcUmsClient, Void, E, W> {

    public static final String UMS_CLIENT = "UMS_CLIENT";

    private GrpcUmsClient umsClient;

    UmsClient(String newId) {
        super(newId);
    }

    UmsClient() {
        this(UMS_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support by ums client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by ums client");
    }

    @Override
    public GrpcUmsClient getDefaultClient() {
        return umsClient;
    }

    public static synchronized UmsClient createProxyUmsClient(Tracer tracer, String umsHost) {
        UmsClient clientEntity = new UmsClient();
        UmsClientConfig clientConfig = new UmsClientConfig();
        Field callingServiceName = ReflectionUtils.findField(UmsClientConfig.class, "callingServiceName");
        ReflectionUtils.makeAccessible(callingServiceName);
        ReflectionUtils.setField(callingServiceName, clientConfig, "cloudbreak");
        Field grpcTimeoutSec = ReflectionUtils.findField(UmsClientConfig.class, "grpcTimeoutSec");
        ReflectionUtils.makeAccessible(grpcTimeoutSec);
        ReflectionUtils.setField(grpcTimeoutSec, clientConfig, 5L);
        clientEntity.umsClient = GrpcUmsClient.createClient(
                UmsChannelConfig.newManagedChannelWrapper(umsHost, 8982), clientConfig, tracer);
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(UmsTestDto.class.getSimpleName(),
                UmsGroupTestDto.class.getSimpleName());
    }
}
