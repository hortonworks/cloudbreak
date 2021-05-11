package com.sequenceiq.environment.environment.service.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.CloudNetworkService;

public class NetworkMetadataValidationServiceTest extends NetworkTest {

    @Mock
    private CloudNetworkService cloudNetworkService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private NetworkMetadataValidationService underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(false);
    }

    @Test
    public void testWithEndpointGatewayAndProvidedSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = createEnvironment(createNetwork());

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);

        Map<String, CloudSubnet> subnetResult =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto));

        assertEquals(2, subnetResult.size());
        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_ID_2), subnetResult.keySet());
    }

    @Test
    public void testWithEndpointGatewayRemovePrivateSubnetsValidationEnabled() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = createEnvironment(createNetwork());

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();
        endpointGatewaySubnets.putAll(createDefaultPrivateSubnets());

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);

        Map<String, CloudSubnet> subnetResult =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto));

        assertEquals(2, subnetResult.size());
        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_ID_2), subnetResult.keySet());
    }

    @Test
    public void testWithEndpointGatewayRemovePrivateSubnetsValidationDisabled() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = createEnvironment(createNetwork());

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();
        endpointGatewaySubnets.putAll(createDefaultPrivateSubnets());

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(true);

        Map<String, CloudSubnet> subnetResult =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto));

        assertEquals(4, subnetResult.size());
        assertEquals(Set.of(ID_1, ID_2, PUBLIC_ID_1, PUBLIC_ID_2), subnetResult.keySet());
    }

    @Test
    public void testWithEndpointGatewayAndEnvironmentSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = createEnvironment(createNetwork());

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        subnets.putAll(createDefaultPublicSubnets());

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(Map.of());

        // Testing that underTest.accept() does not throw a BadRequestException
        Map<String, CloudSubnet> subnetResult =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto));

        assertEquals(0, subnetResult.size());
    }

    @Test
    public void testWithEndpointGatewayAndNoPublicSubnetsProvided() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.getNetwork().setSubnetMetas(createDefaultPrivateSubnets());
        Environment environment = createEnvironment(createNetwork());

        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto)));

        assertTrue(exception.getMessage().startsWith(UNMATCHED_AZ_MSG));
    }

    @Test
    public void testWithEndpointGatewayWithMissingPublicSubnets() {
        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        subnets.put("id3", createPrivateSubnet("id3", "AZ-3"));
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.getNetwork().setSubnetMetas(subnets);
        Environment environment = createEnvironment(createNetwork());

        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEndpointGatewaySubnetMetadata(environment, environmentDto)));

        assertTrue(exception.getMessage().startsWith(UNMATCHED_AZ_MSG));
    }
}
