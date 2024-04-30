package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.common.api.type.PublicEndpointAccessGateway.DISABLED;
import static com.sequenceiq.common.api.type.PublicEndpointAccessGateway.ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class LoadBalancerTypeDeterminerTest {

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LoadBalancerTypeDeterminer underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] scenarios() {
        return new Object[][] {
            //entitled  cloudplatform  PEAG      SubnetMap                            expected
            { false,    AWS,           ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    AZURE,         ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    GCP,           ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    AWS,           DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    AZURE,         DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    GCP,           DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { false,    AWS,           ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    AZURE,         ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    GCP,           ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    AWS,           DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    AZURE,         DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    GCP,           DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { false,    AWS,           ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    AZURE,         ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    GCP,           ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    AWS,           DISABLED, createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    AZURE,         DISABLED, createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    GCP,           DISABLED, createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { false,    AWS,           ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { false,    AZURE,         ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { false,    GCP,           ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { false,    AWS,           DISABLED, null,                                LoadBalancerType.PUBLIC },
            { false,    AZURE,         DISABLED, null,                                LoadBalancerType.PUBLIC },
            { false,    GCP,           DISABLED, null,                                LoadBalancerType.PUBLIC },

            { true,     AWS,           ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { true,     AZURE,         ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { true,     GCP,           ENABLED,  createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { true,     AWS,           DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.PUBLIC },
            { true,     AZURE,         DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.GATEWAY_PRIVATE },
            { true,     GCP,           DISABLED, createSubnetMap(SubnetType.PUBLIC),  LoadBalancerType.GATEWAY_PRIVATE },
            { true,     AWS,           ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { true,     AZURE,         ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { true,     GCP,           ENABLED,  createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.PUBLIC },
            { true,     AWS,           DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.GATEWAY_PRIVATE },
            { true,     AZURE,         DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.GATEWAY_PRIVATE },
            { true,     GCP,           DISABLED, createSubnetMap(SubnetType.PRIVATE), LoadBalancerType.GATEWAY_PRIVATE },
            { true,     AWS,           ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { true,     AZURE,         ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { true,     GCP,           ENABLED,  createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { true,     AWS,           DISABLED, createSubnetMap(null),               LoadBalancerType.PUBLIC },
            { true,     AZURE,         DISABLED, createSubnetMap(null),               LoadBalancerType.GATEWAY_PRIVATE },
            { true,     GCP,           DISABLED, createSubnetMap(null),               LoadBalancerType.GATEWAY_PRIVATE },
            { true,     AWS,           ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { true,     AZURE,         ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { true,     GCP,           ENABLED,  null,                                LoadBalancerType.PUBLIC },
            { true,     AWS,           DISABLED, null,                                LoadBalancerType.PUBLIC },
            { true,     AZURE,         DISABLED, null,                                LoadBalancerType.PUBLIC },
            { true,     GCP,           DISABLED, null,                                LoadBalancerType.PUBLIC },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "Entitled={0}, Platform={1}, PEAG={2}, SubnetMap={3}, expected={4}")
    @MethodSource("scenarios")
    void testGetType(boolean entitlementEnabled, String cloudPlatform, PublicEndpointAccessGateway peag,
            Map<String, CloudSubnet> subnetMap, LoadBalancerType expected) {
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(entitlementEnabled);

        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(peag);
        network.setGatewayEndpointSubnetMetas(subnetMap);

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(cloudPlatform);
        environment.setNetwork(network);

        assertThat(underTest.getType(environment)).isEqualTo(expected);
    }

    private static Map<String, CloudSubnet> createSubnetMap(SubnetType subnetType) {
        CloudSubnet sn1 = new CloudSubnet();
        sn1.setType(subnetType);
        return Map.of("subnet1", sn1);
    }

}
