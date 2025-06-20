package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureOutboundManagerTest {

    @InjectMocks
    private AzureOutboundManager underTest;

    @ParameterizedTest
    @MethodSource("updateNetworkOutboundTestData")
    void updateNetworkOutboundShouldUpdateNetworkAttributesCorrectly(OutboundType initialOutboundType, OutboundType targetOutboundType,
            boolean setInitialAttributes) {
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);

        if (setInitialAttributes) {
            NetworkAttributes networkAttributes = new NetworkAttributes();
            if (initialOutboundType != null) {
                networkAttributes.setOutboundType(initialOutboundType);
            }
            network.setTypedAttributes(networkAttributes);
        }
        // If setInitialAttributes is false, network has no typed attributes set

        CloudResourceStatus result = underTest.updateNetworkOutbound(network, targetOutboundType);

        assertUpdatedNetwork(result, network, targetOutboundType);
    }

    static Stream<Arguments> updateNetworkOutboundTestData() {
        return Stream.of(
                Arguments.of(OutboundType.DEFAULT, OutboundType.LOAD_BALANCER, true,
                        "Should update network attributes when network attributes exist"),
                Arguments.of(null, OutboundType.LOAD_BALANCER, true,
                        "Should return updated status when network attributes are null"),
                Arguments.of(null, OutboundType.PUBLIC_IP, false,
                        "Should create new network attributes when not present")
        );
    }

    private void assertUpdatedNetwork(CloudResourceStatus result, CloudResource network, OutboundType expectedOutboundType) {
        assertEquals(ResourceStatus.UPDATED, result.getStatus());
        assertEquals(network, result.getCloudResource());
        NetworkAttributes updatedAttributes = network.getTypedAttributes(NetworkAttributes.class, NetworkAttributes::new);
        assertEquals(expectedOutboundType, updatedAttributes.getOutboundType());
    }

    @ParameterizedTest
    @MethodSource("outboundTypeTestData")
    void shouldSyncForOutboundShouldReturnExpectedResultBasedOnOutboundType(OutboundType outboundType, boolean expectedResult, String testDescription) {
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        NetworkAttributes networkAttributes = new NetworkAttributes();
        if (outboundType != null) {
            networkAttributes.setOutboundType(outboundType);
        }
        // For null outboundType case, we don't set it, which defaults to NOT_DEFINED in getOutboundType()
        network.setTypedAttributes(networkAttributes);
        List<CloudResource> resources = List.of(network);

        boolean result = underTest.shouldSyncForOutbound(resources);

        assertEquals(expectedResult, result, testDescription);
    }

    static Stream<Arguments> outboundTypeTestData() {
        return Stream.of(
                Arguments.of(OutboundType.NOT_DEFINED, true, "Should return true when outbound type is NOT_DEFINED"),
                Arguments.of(OutboundType.DEFAULT, true, "Should return true when outbound type is DEFAULT"),
                Arguments.of(null, true, "Should return true when outbound type is null (defaults to NOT_DEFINED)"),
                Arguments.of(OutboundType.LOAD_BALANCER, false, "Should return false when outbound type is LOAD_BALANCER"),
                Arguments.of(OutboundType.PUBLIC_IP, false, "Should return false when outbound type is PUBLIC_IP"),
                Arguments.of(OutboundType.USER_ASSIGNED_NATGATEWAY, false, "Should return false when outbound type is USER_ASSIGNED_NATGATEWAY"),
                Arguments.of(OutboundType.USER_DEFINED_ROUTING, false, "Should return false when outbound type is USER_DEFINED_ROUTING")
        );
    }

    @Test
    void shouldSyncForOutboundShouldReturnFalseWhenNoNetworkResourceFound() {
        CloudResource instance = createCloudResource("instance1", AZURE_INSTANCE);
        List<CloudResource> resources = List.of(instance);

        boolean result = underTest.shouldSyncForOutbound(resources);

        assertFalse(result);
    }

    @Test
    void shouldSyncForOutboundShouldReturnFalseWhenResourceListIsEmpty() {
        List<CloudResource> resources = List.of();

        boolean result = underTest.shouldSyncForOutbound(resources);

        assertFalse(result);
    }

    @Test
    void shouldSyncForOutboundShouldHandleNetworkWithoutNetworkAttributes() {
        CloudResource network = createCloudResource("network1", AZURE_NETWORK);
        // No network attributes set, getTypedAttributes will create new NetworkAttributes
        List<CloudResource> resources = List.of(network);

        boolean result = underTest.shouldSyncForOutbound(resources);

        // New NetworkAttributes will have null outboundType, which defaults to NOT_DEFINED
        assertTrue(result);
    }

    @Test
    void shouldSyncForOutboundShouldFindFirstNetworkWhenMultipleNetworksExist() {
        CloudResource network1 = createCloudResource("network1", AZURE_NETWORK);
        NetworkAttributes networkAttributes1 = new NetworkAttributes();
        networkAttributes1.setOutboundType(OutboundType.LOAD_BALANCER);
        network1.setTypedAttributes(networkAttributes1);

        CloudResource network2 = createCloudResource("network2", AZURE_NETWORK);
        NetworkAttributes networkAttributes2 = new NetworkAttributes();
        networkAttributes2.setOutboundType(OutboundType.DEFAULT);
        network2.setTypedAttributes(networkAttributes2);

        List<CloudResource> resources = List.of(network1, network2);

        boolean result = underTest.shouldSyncForOutbound(resources);

        // Should use the first network (network1) with LOAD_BALANCER type
        assertFalse(result);
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(CREATED)
                .withType(resourceType)
                .withInstanceId("instanceId")
                .withGroup("test")
                .build();
    }
}