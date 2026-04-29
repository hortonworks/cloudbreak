package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

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
class AzureResourceGroupTagUpdateStrategyTest {
    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String RESOURCE_NAME = "resourceName";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

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
    private AzureResourceGroupTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
    }

    @Test
    void testUpdateTagsAzureResourceGroup() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_RESOURCE_GROUP, RESOURCE_NAME, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient).updateResourceGroupTags(RESOURCE_NAME, USER_DEFINED_TAGS);
    }

    @Test
    void testUpdateTagsAzureResourceGroupWithoutNewTags() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_RESOURCE_GROUP, RESOURCE_NAME, RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(azureClient.getResourceGroupTags(RESOURCE_NAME)).thenReturn(USER_DEFINED_TAGS);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient, times(0)).updateResourceGroupTags(RESOURCE_NAME, USER_DEFINED_TAGS);
    }

    private CloudResource buildResource(ResourceType type, String resourceName, String reference) {
        return CloudResource.builder()
                .withType(type)
                .withName(resourceName)
                .withInstanceId(null)
                .withReference(reference)
                .withParameters(Collections.emptyMap())
                .build();
    }
}