package com.sequenceiq.cloudbreak.cloud.azure.tag;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureVolumeSetTagUpdateStrategyTest {
    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String RESOURCE_NAME = "resourceName";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final String VOLUME_ID_1 = "volumeId1";

    private static final String VOLUME_ID_2 = "volumeId2";

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
    private AzureVolumeSetTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
    }

    @Test
    void testUpdateTagsAzureVolumeSet() {
        VolumeSetAttributes.Volume volume1 = new VolumeSetAttributes.Volume(VOLUME_ID_1, "ssd", 100, null, null);
        VolumeSetAttributes.Volume volume2 = new VolumeSetAttributes.Volume(VOLUME_ID_2, "ssd", 100, null, null);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-east-1", false, null,
                List.of(volume1, volume2), 100, null);
        CloudResource cloudResource = buildResource(ResourceType.AZURE_VOLUMESET, RESOURCE_NAME, RESOURCE_REFERENCE, volumeSetAttributes);

        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient).updateDiskTags(VOLUME_ID_1, USER_DEFINED_TAGS);
        verify(azureClient).updateDiskTags(VOLUME_ID_2, USER_DEFINED_TAGS);
    }

    @Test
    void testUpdateTagsAzureVolumeSetWithoutNewTags() {
        VolumeSetAttributes.Volume volume1 = new VolumeSetAttributes.Volume(VOLUME_ID_1, "ssd", 100, null, null);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-east-1", false, null,
                List.of(volume1), 100, null);
        CloudResource cloudResource = buildResource(ResourceType.AZURE_VOLUMESET, RESOURCE_NAME, RESOURCE_REFERENCE, volumeSetAttributes);

        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);
        when(azureClient.getDiskTags(VOLUME_ID_1)).thenReturn(USER_DEFINED_TAGS);


        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient,  times(0)).updateDiskTags(VOLUME_ID_1, USER_DEFINED_TAGS);
    }

    private CloudResource buildResource(ResourceType type, String resourceName, String reference, VolumeSetAttributes volumeSetAttributes) {
        return CloudResource.builder()
                .withType(type)
                .withName(resourceName)
                .withInstanceId(null)
                .withReference(reference)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, volumeSetAttributes))
                .build();
    }
}