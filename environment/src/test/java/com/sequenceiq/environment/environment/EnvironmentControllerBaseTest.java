package com.sequenceiq.environment.environment;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.environment.service.EnvironmentCreationService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentUpgradeCcmService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentApiConverter;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;

public abstract class EnvironmentControllerBaseTest {

    protected static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    protected static final Set<String> SUBNETS = Set.of("subnet1", "subnet2");

    protected static final String ENV_CRN = "envCrn";

    @Mock
    private EnvironmentApiConverter mockEnvironmentApiConverter;

    @Mock
    private EnvironmentCreationService mockEnvironmentCreationService;

    @Mock
    private EnvironmentResponseConverter mockEnvironmentResponseConverter;

    @Mock
    private EnvironmentUpgradeCcmService mockUpgradeCcmService;

    @Mock
    private EnvironmentService mockEnvironmentService;

    @Mock
    private StackV4Endpoint mockStackV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory mockInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator mockIam;

    public static Stream<Arguments> ccmScenarios() {
        return Stream.of(
                Arguments.of(Tunnel.DIRECT, 0, false),
                Arguments.of(Tunnel.DIRECT, 1, false),
                Arguments.of(Tunnel.DIRECT, 2, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 0, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 1, false),
                Arguments.of(Tunnel.CLUSTER_PROXY, 2, false),
                Arguments.of(Tunnel.CCM, 0, true),
                Arguments.of(Tunnel.CCM, 1, true),
                Arguments.of(Tunnel.CCM, 2, true),
                Arguments.of(Tunnel.CCMV2, 0, true),
                Arguments.of(Tunnel.CCMV2, 1, true),
                Arguments.of(Tunnel.CCMV2, 2, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 0, false),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 1, true),
                Arguments.of(Tunnel.CCMV2_JUMPGATE, 2, true)
        );
    }

    protected EnvironmentApiConverter getMockEnvironmentApiConverter() {
        return mockEnvironmentApiConverter;
    }

    protected EnvironmentCreationService getMockEnvironmentCreationService() {
        return mockEnvironmentCreationService;
    }

    protected EnvironmentResponseConverter getMockEnvironmentResponseConverter() {
        return mockEnvironmentResponseConverter;
    }

    protected EnvironmentUpgradeCcmService getMockUpgradeCcmService() {
        return mockUpgradeCcmService;
    }

    protected EnvironmentService getMockEnvironmentService() {
        return mockEnvironmentService;
    }

    protected StackV4Endpoint getMockStackV4Endpoint() {
        return mockStackV4Endpoint;
    }

    protected RegionAwareInternalCrnGeneratorFactory getMockInternalCrnGeneratorFactory() {
        return mockInternalCrnGeneratorFactory;
    }

    protected RegionAwareInternalCrnGenerator getMockIam() {
        return mockIam;
    }

    protected EnvironmentNetworkRequest setupNetworkRequestWithEndpointGatway() {
        EnvironmentNetworkRequest networkRequest = new EnvironmentNetworkRequest();
        networkRequest.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        networkRequest.setEndpointGatewaySubnetIds(SUBNETS);
        return networkRequest;
    }

    protected abstract void setupServiceResponses();

}
