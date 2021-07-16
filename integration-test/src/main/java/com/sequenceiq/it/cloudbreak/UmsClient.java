package com.sequenceiq.it.cloudbreak;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsChannelConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

import io.opentracing.Tracer;

public class UmsClient extends MicroserviceClient<GrpcUmsClient, Void> {

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
    public <E extends Enum<E>, W extends WaitObject> W waitObject(CloudbreakTestDto entity, String name, Map<String, E> desiredStatuses,
            TestContext testContext) {
        throw new TestFailException("Wait object does not support by ums client");
    }

    @Override
    public GrpcUmsClient getDefaultClient() {
        return umsClient;
    }

    public static synchronized UmsClient createProxyUmsClient(Tracer tracer, String umsHost) {
        UmsClient clientEntity = new UmsClient();
        UmsClientConfig clientConfig = new UmsClientConfig();
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
