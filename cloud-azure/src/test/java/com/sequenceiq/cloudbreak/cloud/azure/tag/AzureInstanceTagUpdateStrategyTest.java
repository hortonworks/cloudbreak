package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AzureInstanceTagUpdateStrategyTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

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
    private AzureInstanceTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
    }

    @Test
    void testDeleteTagsAzureInstance() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(azureClient.getVirtualMachineTags(RESOURCE_REFERENCE)).thenReturn(EXISTING_TAGS);

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verify(azureClient).updateVirtualMachineTags(RESOURCE_REFERENCE, Map.of());
    }

    @Test
    void testDeleteTagsSkipWhenKeyNotPresent() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(azureClient.getVirtualMachineTags(RESOURCE_REFERENCE)).thenReturn(Map.of("otherKey", "otherValue"));

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verify(azureClient, times(0)).updateVirtualMachineTags(RESOURCE_REFERENCE, Map.of());
    }

    @Test
    void testUpdateTagsAzureInstance() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient).updateVirtualMachineTags(RESOURCE_REFERENCE, USER_DEFINED_TAGS);
    }

    @Test
    void testUpdateTagsAzureInstanceWithoutNewTags() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(azureClient.getVirtualMachineTags(RESOURCE_REFERENCE)).thenReturn(USER_DEFINED_TAGS);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient, times(0)).updateVirtualMachineTags(RESOURCE_REFERENCE, USER_DEFINED_TAGS);
    }

    @Test
    void testUpdateTagsAzureInstanceSkippedWhenReferenceIsNull() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, null);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoInteractions(azureClient);
    }

    @Test
    void testDeleteTagsAzureInstanceSkippedWhenReferenceIsNull() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, null, null);

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verifyNoInteractions(azureClient);
    }

    private CloudResource buildResource(ResourceType type, String instanceId, String reference) {
        return CloudResource.builder()
                .withType(type)
                .withName(type.name().toLowerCase())
                .withInstanceId(instanceId)
                .withReference(reference)
                .withParameters(Collections.emptyMap())
                .build();
    }
}