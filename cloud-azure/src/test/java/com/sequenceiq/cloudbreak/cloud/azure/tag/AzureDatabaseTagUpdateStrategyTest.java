package com.sequenceiq.cloudbreak.cloud.azure.tag;

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
class AzureDatabaseTagUpdateStrategyTest {

    private static final String FLEXIBLE_SERVER_RESOURCE_REFERENCE =
            "/subscriptions/dummySubscription/resourceGroups/dummyResourceGroup/providers/Microsoft.DBforPostgreSQL/flexibleServers/dbId";

    private static final String SINGLE_SERVER_RESOURCE_REFERENCE =
            "/subscriptions/dummySubscription/resourceGroups/dummyResourceGroup/providers/Microsoft.DBforPostgreSQL/servers/dbId";

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
    private AzureDatabaseResourceTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
    }

    @Test
    void testUpdateTagsAzureFlexibleDatabaseServer() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_DATABASE, null, FLEXIBLE_SERVER_RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient).updateFlexibleServerTags(FLEXIBLE_SERVER_RESOURCE_REFERENCE, USER_DEFINED_TAGS);
    }

    @Test
    void testUpdateTagsAzureSingleDatabaseServer() {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_DATABASE, null, SINGLE_SERVER_RESOURCE_REFERENCE);
        when(azureClientService.getClient(cloudContext, cloudCredential)).thenReturn(azureClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(azureClient).updateSingleServerTags(SINGLE_SERVER_RESOURCE_REFERENCE, USER_DEFINED_TAGS);
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