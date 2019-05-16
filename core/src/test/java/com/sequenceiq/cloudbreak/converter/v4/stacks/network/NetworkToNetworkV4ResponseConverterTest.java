package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
public class NetworkToNetworkV4ResponseConverterTest {

    private static final String DEFAULT_CIDR = "10.0.0.0/16";

    @Mock
    private Stack stack;

    @Mock
    private Network network;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private NetworkToNetworkV4ResponseConverter underTest;

    @Test
    public void testConvertShouldReturnNullWhenTheStackDoesNotHaveAssignedNetwork() {
        when(stack.getNetwork()).thenReturn(null);

        NetworkV4Response result = underTest.convert(stack);

        assertNull(result);
    }

    @Test
    public void testConvertShouldReturnResponseWithSubnetCIDRWhenTheStackNetworkDoesNotContainAttributes() {
        when(network.getSubnetCIDR()).thenReturn(DEFAULT_CIDR);
        when(stack.getNetwork()).thenReturn(network);
        when(network.getAttributes()).thenReturn(null);

        NetworkV4Response result = underTest.convert(stack);

        assertNotNull(result);
        assertEquals(DEFAULT_CIDR, result.getSubnetCIDR());
    }

    @Test
    public void testConvertShouldRCallTheParserWhenTheStackStackDoesNotContainNetworkRelatedResources() throws JsonProcessingException {
        Resource gcpDisk = new Resource();
        gcpDisk.setResourceType(ResourceType.GCP_DISK);
        gcpDisk.setResourceName("myproject-attachedworkerdisk");
        when(stack.getResources()).thenReturn(Set.of(gcpDisk));
        when(stack.getNetwork()).thenReturn(network);

        Map<String, Object> attributes = Map.of("cloudPlatform", "GCP");
        when(network.getAttributes()).thenReturn(new Json(attributes));
        when(network.getSubnetCIDR()).thenReturn(DEFAULT_CIDR);

        NetworkV4Response result = underTest.convert(stack);

        verify(providerParameterCalculator).parse(eq(attributes), any());
        assertNotNull(result);
        assertEquals(DEFAULT_CIDR, result.getSubnetCIDR());
    }

    @Test
    public void testConvertShouldRCallTheParserWhenTheStackStackContainsNetworkRelatedResources() throws JsonProcessingException {
        Resource vpc = new Resource();
        vpc.setResourceType(ResourceType.AWS_VPC);
        vpc.setResourceName("idOfTheCreatedVPC");
        Resource subnet = new Resource();
        subnet.setResourceType(ResourceType.AWS_SUBNET);
        subnet.setResourceName("idOfTheCreatedSubnet");
        Resource azureNetwork = new Resource();
        azureNetwork.setResourceType(ResourceType.AZURE_NETWORK);
        azureNetwork.setResourceName("idOfTheCreatedAzureNetwork");
        when(stack.getResources()).thenReturn(Set.of(vpc, subnet, azureNetwork));
        when(stack.getNetwork()).thenReturn(network);

        Map<String, Object> attributes = Map.of("cloudPlatform", "MOCK");
        when(network.getAttributes()).thenReturn(new Json(attributes));
        when(network.getSubnetCIDR()).thenReturn(DEFAULT_CIDR);

        NetworkV4Response result = underTest.convert(stack);

        Map<String, Object> expectedParameters = new HashMap<>(attributes);
        expectedParameters.put("vpcId", vpc.getResourceName());
        expectedParameters.put("subnetId", subnet.getResourceName());
        expectedParameters.put("networkId", azureNetwork.getResourceName());
        verify(providerParameterCalculator).parse(eq(expectedParameters), any());
        assertNotNull(result);
        assertEquals(DEFAULT_CIDR, result.getSubnetCIDR());
    }
}