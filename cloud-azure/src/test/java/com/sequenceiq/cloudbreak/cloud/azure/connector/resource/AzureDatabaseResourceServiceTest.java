package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.common.api.type.CommonStatus;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseResourceServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resource group name";

    @Mock
    private AzureTemplateBuilder azureTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AzureClient client;

    @Mock
    private ResourceGroup resourceGroup;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @InjectMocks
    private AzureDatabaseResourceService victim;

    @BeforeEach
    void initTests() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(ac.getParameter(AzureClient.class)).thenReturn(client);
    }

    @Test
    void shouldReturnDeletedStatusInCaseOfMissingResourceGroup() {
        when(client.getResourceGroup(RESOURCE_GROUP_NAME)).thenReturn(null);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);

        ExternalDatabaseStatus actual = victim.getDatabaseServerStatus(ac, databaseStack);

        assertEquals(ExternalDatabaseStatus.DELETED, actual);
    }

    @Test
    void shouldReturnStartedStatusInCaseOfExistingResourceGroup() {
        when(client.getResourceGroup(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);

        ExternalDatabaseStatus actual = victim.getDatabaseServerStatus(ac, databaseStack);

        assertEquals(ExternalDatabaseStatus.STARTED, actual);
    }

    @Test
    void shouldReturnDeletedDbServerWhenTerminateDatabaseServerAndSingleResourceGroup() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(azureResourceGroupMetadataProvider.useSingleResourceGroup(any(DatabaseStack.class))).thenReturn(true);
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .type(AZURE_DATABASE)
                        .reference("dbReference")
                        .name("dbName")
                        .status(CommonStatus.CREATED)
                        .params(Map.of())
                        .build());

        List<CloudResourceStatus> resourceStatuses = victim.terminateDatabaseServer(ac, databaseStack, cloudResources, false);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_DATABASE, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteDatabaseServer(any(), eq("dbReference"));
        verify(client, never()).deleteResourceGroup(anyString());
    }

    @Test
    void shouldReturnDeletedResourceGroupWhenTerminateDatabaseServerAndMultipleResourceGroups() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(azureResourceGroupMetadataProvider.useSingleResourceGroup(any(DatabaseStack.class))).thenReturn(false);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .type(AZURE_DATABASE)
                        .reference("dbReference")
                        .name("dbName")
                        .status(CommonStatus.CREATED)
                        .params(Map.of())
                        .build());

        List<CloudResourceStatus> resourceStatuses = victim.terminateDatabaseServer(ac, databaseStack, cloudResources, false);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_RESOURCE_GROUP, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(client).deleteResourceGroup(eq(RESOURCE_GROUP_NAME));
        verify(azureUtils, never()).deleteDatabaseServer(any(), anyString());
    }
}