package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureSingleResourceTagUpdateStrategyTest {

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String RESOURCE_NAME = "resourceName";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final Map<String, String> EXISTING_TAGS = Map.of("existingKey", "existingValue");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @InjectMocks
    private AzureSingleResourceTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
    }

    private static Stream<Arguments> resourceTypes() {
        return Stream.of(
                resourceTypeArgs(ResourceType.AZURE_INSTANCE, false, AzureClient::getVirtualMachineTags, AzureClient::updateVirtualMachineTags),
                resourceTypeArgs(ResourceType.AZURE_DISK, false, AzureClient::getDiskTags, AzureClient::updateDiskTags),
                resourceTypeArgs(ResourceType.AZURE_LOAD_BALANCER, false, AzureClient::getLoadBalancerTags, AzureClient::updateLoadBalancerTags),
                resourceTypeArgs(ResourceType.AZURE_NETWORK_INTERFACE, false,
                        AzureClient::getNetworkInterfaceTags, AzureClient::updateNetworkInterfaceTags),
                resourceTypeArgs(ResourceType.AZURE_SECURITY_GROUP, false,
                        AzureClient::getNetworkSecurityGroupTags, AzureClient::updateNetworkSecurityGroupTags),
                resourceTypeArgs(ResourceType.AZURE_PUBLIC_IP, false, AzureClient::getPublicIpTags, AzureClient::updatePublicIpTags),
                resourceTypeArgs(ResourceType.AZURE_AVAILABILITY_SET, false,
                        AzureClient::getAvailabilitySetTags, AzureClient::updateAvailabilitySetTags),
                resourceTypeArgs(ResourceType.AZURE_RESOURCE_GROUP, true, AzureClient::getResourceGroupTags, AzureClient::updateResourceGroupTags));
    }

    private static Arguments resourceTypeArgs(ResourceType type, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        return Arguments.of(type, nameBased, tagGetter, tagUpdater);
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testDeleteTags(ResourceType type, boolean nameBased, BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        String resourceId = nameBased ? RESOURCE_NAME : RESOURCE_REFERENCE;
        CloudResource cloudResource = buildResource(type, nameBased, resourceId);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(tagGetter.apply(azureClient, resourceId)).thenReturn(EXISTING_TAGS);

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        tagUpdater.update(verify(azureClient), resourceId, Map.of());
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testDeleteTagsSkipWhenKeyNotPresent(ResourceType type, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        String resourceId = nameBased ? RESOURCE_NAME : RESOURCE_REFERENCE;
        CloudResource cloudResource = buildResource(type, nameBased, resourceId);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(tagGetter.apply(azureClient, resourceId)).thenReturn(Map.of("otherKey", "otherValue"));

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        tagUpdater.update(verify(azureClient, times(0)), resourceId, Map.of());
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testUpdateTags(ResourceType type, boolean nameBased, BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        String resourceId = nameBased ? RESOURCE_NAME : RESOURCE_REFERENCE;
        CloudResource cloudResource = buildResource(type, nameBased, resourceId);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        tagUpdater.update(verify(azureClient), resourceId, USER_DEFINED_TAGS);
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testUpdateTagsWithoutNewTags(ResourceType type, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        String resourceId = nameBased ? RESOURCE_NAME : RESOURCE_REFERENCE;
        CloudResource cloudResource = buildResource(type, nameBased, resourceId);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(tagGetter.apply(azureClient, resourceId)).thenReturn(USER_DEFINED_TAGS);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        tagUpdater.update(verify(azureClient, times(0)), resourceId, USER_DEFINED_TAGS);
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testUpdateTagsSkippedWhenIdIsNull(ResourceType type, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        CloudResource cloudResource = buildResource(type, nameBased, null);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verifyNoInteractions(azureClient);
    }

    @ParameterizedTest
    @MethodSource("resourceTypes")
    void testDeleteTagsSkippedWhenIdIsNull(ResourceType type, boolean nameBased,
            BiFunction<AzureClient, String, Map<String, String>> tagGetter, TagUpdater tagUpdater) {
        CloudResource cloudResource = buildResource(type, nameBased, null);

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verifyNoInteractions(azureClient);
    }

    private CloudResource buildResource(ResourceType type, boolean nameBased, String resourceId) {
        // CloudResource requires a non-null name, so a blank name is used in place of a null one to exercise the
        // "resource ID is missing" scenario for name-based resources (e.g. resource groups).
        String name = nameBased ? (resourceId == null ? " " : resourceId) : type.name().toLowerCase();
        return CloudResource.builder()
                .withType(type)
                .withName(name)
                .withInstanceId(null)
                .withReference(nameBased ? RESOURCE_REFERENCE : resourceId)
                .withParameters(Collections.emptyMap())
                .build();
    }

    @FunctionalInterface
    private interface TagUpdater {
        void update(AzureClient azureClient, String resourceId, Map<String, String> tags);
    }
}
