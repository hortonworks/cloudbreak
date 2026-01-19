package com.sequenceiq.it.cloudbreak.microservice;

import java.lang.reflect.Field;
import java.util.Set;

import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.GrpcAuthDistributorClient;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.authdistributor.FetchAuthViewTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

public class AuthDistributorClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcAuthDistributorClient, Void, E, W> {

    private GrpcAuthDistributorClient grpcAuthDistributorClient;

    public  AuthDistributorClient(
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory, String host) {
        AuthDistributorConfig authDistributorConfig = new AuthDistributorConfig();
        Field grpcTimeoutSec = ReflectionUtils.findField(AuthDistributorConfig.class, "grpcTimeoutSec");
        ReflectionUtils.makeAccessible(grpcTimeoutSec);
        ReflectionUtils.setField(grpcTimeoutSec, authDistributorConfig, 120);
        grpcAuthDistributorClient = GrpcAuthDistributorClient.createClient(
                AuthDistributorConfig.newManagedChannelWrapper(host, 8982), authDistributorConfig, regionAwareInternalCrnGeneratorFactory);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint(TestContext testContext) {
        throw new TestFailException("Flow does not support by auth distributor client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by auth distributor client");
    }

    @Override
    public GrpcAuthDistributorClient getDefaultClient(TestContext testContext) {
        return grpcAuthDistributorClient;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(FetchAuthViewTestDto.class.getSimpleName());
    }
}
