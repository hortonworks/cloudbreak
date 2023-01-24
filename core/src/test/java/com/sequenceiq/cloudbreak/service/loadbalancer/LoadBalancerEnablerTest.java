package com.sequenceiq.cloudbreak.service.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class LoadBalancerEnablerTest {

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LoadBalancerEnabler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "supportedPlatforms", "AWS,AZURE");
        underTest.init();
    }

    @ParameterizedTest(name = "StackType = {0}, Cloud = {1}, PEAG = {2}, Subnets = {3}, EnvLBCreation = {4}, EnableStackLb = {5} then result is {6}")
    @MethodSource("isLoadBalancerEnabledScenarios")
    void isLoadBalancerEnabled(StackType stackType, String cloudPlatform, PublicEndpointAccessGateway peag, Set<String> subnetIds,
            LoadBalancerCreation networkLoadBalancerCreation, boolean enableLoadBalancerOnStack, boolean expected) {
        DetailedEnvironmentResponse environment = prepareEnvironment(cloudPlatform, peag, subnetIds, networkLoadBalancerCreation);
        assertThat(underTest.isLoadBalancerEnabled(stackType, cloudPlatform, environment, enableLoadBalancerOnStack)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "PEAG = {0}, Subnets = {1}, TargetingEntitled = {2}, then result is {3}")
    @MethodSource("isEndpointGatewayEnabledScenarios")
    void isEndpointGatewayEnabled(PublicEndpointAccessGateway peag, Set<String> subnetIds, boolean targetingEntitled, boolean expected) {
        EnvironmentNetworkResponse network = createNetwork(peag, subnetIds);
        lenient().when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(ACCOUNT_ID)).thenReturn(targetingEntitled);
        assertThat(underTest.isEndpointGatewayEnabled(ACCOUNT_ID, network)).isEqualTo(expected);
    }

    private DetailedEnvironmentResponse prepareEnvironment(String cloudPlatform, PublicEndpointAccessGateway peag, Set<String> subnetIds,
            LoadBalancerCreation loadBalancerCreation) {
        EnvironmentNetworkResponse network = EnvironmentNetworkResponse.builder()
                .withLoadBalancerCreation(loadBalancerCreation)
                .withUsePublicEndpointAccessGateway(peag)
                .withEndpointGatewaySubnetIds(subnetIds)
                .build();
        return DetailedEnvironmentResponse.builder()
                .withNetwork(network)
                .withCloudPlatform(cloudPlatform)
                .withAccountId(ACCOUNT_ID)
                .build();
    }

    private EnvironmentNetworkResponse createNetwork(PublicEndpointAccessGateway peag, Set<String> subnetIds) {
        return EnvironmentNetworkResponse.builder()
                .withUsePublicEndpointAccessGateway(peag)
                .withEndpointGatewaySubnetIds(subnetIds)
                .build();
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] isLoadBalancerEnabledScenarios() {
        // note: there would be 2⁶ or 2⁷ or even more combination of input paramters
        return new Object[][] {
            { StackType.TEMPLATE, "AWS",     PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  true,  false },
            { StackType.WORKLOAD, "UNKNOWN", PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  true,  false },
            { StackType.WORKLOAD, "AWS",     PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.DISABLED, true,  false },
            { StackType.WORKLOAD, "AWS",     PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  false, false },
            { StackType.WORKLOAD, "AWS",     PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  true,  true },
            { StackType.WORKLOAD, "AWS",     PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  false, true },
            { StackType.DATALAKE, "UNKNOWN", PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  true,  false },
            { StackType.DATALAKE, "AWS",     PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.DISABLED, true,  false },
            { StackType.DATALAKE, "AWS",     PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  false, true },
            { StackType.DATALAKE, "AZURE",   PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  false, false },
            { StackType.DATALAKE, "AWS",     PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  true,  true },
            { StackType.DATALAKE, "AWS",     PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), LoadBalancerCreation.ENABLED,  false, true },
        };
    }

    static Object[][] isEndpointGatewayEnabledScenarios() {
        return new Object[][] {
            { null,                                 null,                         false, false },
            { null,                                 null,                         true,  false },
            { null,                                 Set.of("subnet1", "subnet2"), false, false },
            { null,                                 Set.of("subnet1", "subnet2"), true,  true },
            { PublicEndpointAccessGateway.DISABLED, null,                         false, false },
            { PublicEndpointAccessGateway.DISABLED, null,                         true,  false },
            { PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), false, false },
            { PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), true,  true },
            { PublicEndpointAccessGateway.ENABLED,  null,                         false, true },
            { PublicEndpointAccessGateway.ENABLED,  null,                         true,  true },
            { PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), false, true },
            { PublicEndpointAccessGateway.ENABLED,  Set.of("subnet1", "subnet2"), true, true },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
