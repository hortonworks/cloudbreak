package com.sequenceiq.cloudbreak.controller.validation.loadbalancer;

import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_BASE_SUBNET;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_BASE_SUBNET_META;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_USABLE_SUBNET_IN_CLUSTER;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class EndpointGatewayNetworkValidatorTest extends SubnetTest {

    private static final String KEY = "key";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private EndpointGatewayNetworkValidator underTest;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private EntitlementService entitlementService;

    @Test
    void validateNoNetworkProvided() {
        ValidationResult result = underTest.validate(new ImmutablePair<>("", null));

        assertNoError(result);
    }

    @Test
    void validateEndpointGatewayDisabledAndNoEntitlement() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);

        ValidationResult result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(new ImmutablePair<>("", network)));

        assertNoError(result);
    }

    @Test
    void validateEndpointGatewayDisabledEntitledButNoEndpointGwSubnets() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(true);

        ValidationResult result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(new ImmutablePair<>("", network)));

        assertNoError(result);
    }

    @Test
    void validateNoBaseSubnetId() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        ValidationResult result = underTest.validate(new ImmutablePair<>("", network));

        assertThat(result.hasError()).isTrue();
        assertEquals(NO_BASE_SUBNET, result.getErrors().get(0));
    }

    @Test
    void validateNoBaseSubnetMeta() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertThat(result.hasError()).isTrue();
        assertEquals(String.format(NO_BASE_SUBNET_META, PRIVATE_ID_1), result.getErrors().get(0));
    }

    @Test
    void validateProvidedEndpointGatwaySubnets() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        network.setEndpointGatewaySubnetIds(Set.of(PUBLIC_ID_1));
        network.setGatewayEndpointSubnetMetas(Map.of(KEY, publicSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.of(publicSubnet));

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertNoError(result);
    }

    @Test
    void validateEndpointGatewayDisabledEntitledAndEndpointGwSubnets() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(true);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        network.setEndpointGatewaySubnetIds(Set.of(PUBLIC_ID_1));
        network.setGatewayEndpointSubnetMetas(Map.of(KEY, publicSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.of(publicSubnet));

        ValidationResult result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network)));

        assertNoError(result);
    }

    @Test
    void validateWhenEndpointGatewaySubnetsAreInvalid() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        network.setEndpointGatewaySubnetIds(Set.of(PUBLIC_ID_1));
        network.setGatewayEndpointSubnetMetas(Map.of(KEY, publicSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertThat(result.hasError()).isTrue();
        assertEquals(String.format(NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY, AZ_1), result.getErrors().get(0));
    }

    @Test
    void validateProvidedClusterSubnets() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        CloudSubnet privateSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        network.setSubnetMetas(Map.of(KEY, privateSubnet, KEY + '2', publicSubnet));

        when(subnetSelector.findSubnetById(anyMap(), anyString())).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.of(publicSubnet));

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertNoError(result);
    }

    @Test
    void validateWhenClusterSubnetsAreInvalid() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet privateSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        network.setSubnetMetas(Map.of(KEY, privateSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertThat(result.hasError()).isTrue();
        assertEquals(String.format(NO_USABLE_SUBNET_IN_CLUSTER, AZ_1), result.getErrors().get(0));
    }

    private void assertNoError(ValidationResult result) {
        assertThat(result.getState()).isNotEqualTo(ValidationResult.State.ERROR);
        assertThat(result.hasError()).isFalse();
    }
}
