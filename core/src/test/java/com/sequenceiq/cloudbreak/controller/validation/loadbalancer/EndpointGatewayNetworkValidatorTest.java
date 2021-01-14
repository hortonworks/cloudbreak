package com.sequenceiq.cloudbreak.controller.validation.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_BASE_SUBNET;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_BASE_SUBNET_META;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_USABLE_SUBNET_IN_CLUSTER;
import static com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator.NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@RunWith(MockitoJUnitRunner.class)
public class EndpointGatewayNetworkValidatorTest extends SubnetTest {

    private static final String KEY = "key";

    @InjectMocks
    private EndpointGatewayNetworkValidator underTest;

    @Mock
    private SubnetSelector subnetSelector;

    @Test
    public void validateNoNetworkProvided() {
        ValidationResult result = underTest.validate(new ImmutablePair<>("", null));

        assertNoError(result);
    }

    @Test
    public void validateEndpointGatewayDisabled() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);

        ValidationResult result = underTest.validate(new ImmutablePair<>("", network));

        assertNoError(result);
    }

    @Test
    public void validateNoBaseSubnetId() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        ValidationResult result = underTest.validate(new ImmutablePair<>("", network));

        assert result.hasError();
        assertEquals(NO_BASE_SUBNET, result.getErrors().get(0));
    }

    @Test
    public void validateNoBaseSubnetMeta() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assert result.hasError();
        assertEquals(String.format(NO_BASE_SUBNET_META, PRIVATE_ID_1), result.getErrors().get(0));
    }

    @Test
    public void validateProvidedEndpointGatwaySubnets() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        network.setGatewayEndpointSubnetMetas(Map.of(KEY, publicSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.of(publicSubnet));

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assertNoError(result);
    }

    @Test
    public void validateWhenEndpointGatewaySubnetsAreInvalid() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        network.setGatewayEndpointSubnetMetas(Map.of(KEY, publicSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assert result.hasError();
        assertEquals(String.format(NO_USABLE_SUBNET_IN_ENDPOINT_GATEWAY, AZ_1), result.getErrors().get(0));
    }

    @Test
    public void validateProvidedClusterSubnets() {
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
    public void validateWhenClusterSubnetsAreInvalid() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet privateSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        network.setSubnetMetas(Map.of(KEY, privateSubnet));

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());

        ValidationResult result = underTest.validate(new ImmutablePair<>(PRIVATE_ID_1, network));

        assert result.hasError();
        assertEquals(String.format(NO_USABLE_SUBNET_IN_CLUSTER, AZ_1), result.getErrors().get(0));
    }

    private void assertNoError(ValidationResult result) {
        assert result.getState() != ValidationResult.State.ERROR;
        assert !result.hasError();
    }
}
