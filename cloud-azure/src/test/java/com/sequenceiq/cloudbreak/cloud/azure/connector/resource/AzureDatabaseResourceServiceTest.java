package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;

@ExtendWith(MockitoExtension.class)
public class AzureDatabaseResourceServiceTest {

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
    public void initTests() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(ac.getParameter(AzureClient.class)).thenReturn(client);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
    }

    @Test
    public void shouldReturnDeletedStatusInCaseOfMissingResourceGroup() {
        when(client.getResourceGroup(RESOURCE_GROUP_NAME)).thenReturn(null);

        ExternalDatabaseStatus actual = victim.getDatabaseServerStatus(ac, databaseStack);

        assertEquals(ExternalDatabaseStatus.DELETED, actual);
    }

    @Test
    public void shouldReturnStartedStatusInCaseOfExistingResourceGroup() {
        when(client.getResourceGroup(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);

        ExternalDatabaseStatus actual = victim.getDatabaseServerStatus(ac, databaseStack);

        assertEquals(ExternalDatabaseStatus.STARTED, actual);
    }
}