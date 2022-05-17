package com.sequenceiq.it.cloudbreak;

import java.util.Set;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.GrpcAuthDistributorClient;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.dto.authdistributor.FetchAuthViewTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;

import io.opentracing.Tracer;

public class AuthDistributorClient<E extends Enum<E>, W extends WaitObject> extends MicroserviceClient<GrpcAuthDistributorClient, Void, E, W> {

    public static final String AUTH_DISTRIBUTOR_CLIENT = "AUTH_DISTRIBUTOR_CLIENT";

    private GrpcAuthDistributorClient grpcAuthDistributorClient;

    AuthDistributorClient(String newId) {
        super(newId);
    }

    AuthDistributorClient() {
        this(AUTH_DISTRIBUTOR_CLIENT);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        throw new TestFailException("Flow does not support by auth distributor client");
    }

    @Override
    public <T extends WaitObject> WaitService<T> waiterService() {
        throw new TestFailException("Wait service does not support by auth distributor client");
    }

    @Override
    public GrpcAuthDistributorClient getDefaultClient() {
        return grpcAuthDistributorClient;
    }

    public static synchronized AuthDistributorClient createProxyAuthDistributorClient(Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory, String host) {
        AuthDistributorClient clientEntity = new AuthDistributorClient();
        clientEntity.grpcAuthDistributorClient = GrpcAuthDistributorClient.createClient(
                AuthDistributorConfig.newManagedChannelWrapper(host, 8982), tracer, regionAwareInternalCrnGeneratorFactory);
        return clientEntity;
    }

    @Override
    public Set<String> supportedTestDtos() {
        return Set.of(FetchAuthViewTestDto.class.getSimpleName());
    }
}
