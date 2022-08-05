package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.DB_VERSION;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.ResourceGroupUsage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseResourceServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resource group name";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "aStack";

    private static final String RESOURCE_REFERENCE = "aReference";

    @Mock
    private AzureDatabaseTemplateBuilder azureTemplateBuilder;

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

    @Mock
    private PersistenceRetriever persistenceRetriever;

    @Mock
    private Deployment deployment;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @InjectMocks
    private AzureDatabaseResourceService victim;

    @BeforeEach
    void initTests() {
        when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(ac.getParameter(AzureClient.class)).thenReturn(client);
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
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureUtils.deleteDatabaseServer(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));

        List<CloudResourceStatus> resourceStatuses = victim.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_DATABASE, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteDatabaseServer(any(), eq(RESOURCE_REFERENCE), anyBoolean());
        verify(client, never()).deleteResourceGroup(anyString());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldReturnDeletedResourceGroupWhenTerminateDatabaseServerAndMultipleResourceGroups() {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.MULTIPLE);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.deleteResourceGroup(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));

        List<CloudResourceStatus> resourceStatuses = victim.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_RESOURCE_GROUP, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteResourceGroup(any(), eq(RESOURCE_GROUP_NAME), eq(false));
        verify(azureUtils, never()).deleteDatabaseServer(any(), anyString(), anyBoolean());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldUpgradeDatabaseWhenUpgradeDatabaseServerAndPrivateEndpoint() {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = buildDatabaseServer();

        CloudResource dbResource = buildResource(AZURE_DATABASE);
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT);

        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(peResource, dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.of(dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.of(peResource));

        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        victim.upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11);

        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureUtils).deletePrivateEndpoint(client, RESOURCE_REFERENCE, false);
        InOrder inOrder = inOrder(persistenceRetriever);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureTemplateBuilder).build(eq(cloudContext), databaseStackArgumentCaptor.capture());
        assertEquals("11", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        inOrder = inOrder(persistenceNotifier);
        inOrder.verify(persistenceNotifier).notifyUpdate(dbResource, cloudContext);
        inOrder.verify(persistenceNotifier, times(2)).notifyUpdate(peResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyUpdate(dbResource, cloudContext);
    }

    @Test
    void shouldUpgradeDatabaseWhenUpgradeDatabaseServerAndNoPrivateEndpoint() {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        CloudResource dbResource = buildResource(AZURE_DATABASE);
        DatabaseServer databaseServer = buildDatabaseServer();

        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.of(dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.empty());
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        victim.upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11);

        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureUtils, never()).deletePrivateEndpoint(client, RESOURCE_REFERENCE, false);
        InOrder inOrder = inOrder(persistenceRetriever);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureTemplateBuilder).build(eq(cloudContext), databaseStackArgumentCaptor.capture());
        assertEquals("11", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier, times(2)).notifyUpdate(dbResource, cloudContext);
    }

    @Test
    void shouldReturnExceptionWhenUpgradeDatabaseServerThrowsCloudException() {
        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        CloudResource dbResource = buildResource(AZURE_DATABASE);

        when(cloudContext.getId()).thenReturn(STACK_ID);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.of(dbResource));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID))
                .thenReturn(Optional.empty());

        doThrow(new RuntimeException("delete failed")).when(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> victim.upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));

        assertEquals("Error in upgrading database stack aStack: delete failed", exception.getMessage());
        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureUtils, never()).deletePrivateEndpoint(client, RESOURCE_REFERENCE, false);
        InOrder inOrder = inOrder(persistenceRetriever);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID);
        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureTemplateBuilder, never()).build(eq(cloudContext), any(DatabaseStack.class));
        verify(persistenceNotifier, times(1)).notifyUpdate(dbResource, cloudContext);
    }

//    @Test
//    void shouldReturnExceptionWhenUpgradeDatabaseServerDbResourceIsNotFound() {
//        PersistenceNotifier persistenceNotifier = mock(PersistenceNotifier.class);
//        DatabaseStack databaseStack = mock(DatabaseStack.class);
//
//        when(cloudContext.getId()).thenReturn(STACK_ID);
//        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID)).thenReturn(Optional.empty());
//        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID)).thenReturn(Optional.empty());
//
//        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
//                () -> victim.upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, TargetMajorVersion.VERSION_11));
//
//        assertEquals("Azure database server cloud resource does not exist for stack 1!", exception.getMessage());
//        verify(azureUtils, never()).getStackName(eq(cloudContext));
//        verify(azureUtils, never()).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
//        verify(azureUtils, never()).deletePrivateEndpoint(client, RESOURCE_REFERENCE, false);
//        verify(azureResourceGroupMetadataProvider, never()).getResourceGroupName(cloudContext, databaseStack);
//        verify(azureTemplateBuilder, never()).build(eq(cloudContext), any(DatabaseStack.class));
//
//        InOrder inOrder = inOrder(persistenceRetriever);
//        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_DATABASE, CommonStatus.CREATED, STACK_ID);
//        inOrder.verify(persistenceRetriever).retrieveFirstByTypeAndStatusForStack(AZURE_PRIVATE_ENDPOINT, CommonStatus.CREATED, STACK_ID);
//    }

    private CloudResource buildResource(ResourceType resourceType) {
        return CloudResource.builder()
                .withType(resourceType)
                .withReference(RESOURCE_REFERENCE)
                .withName("name")
                .withStatus(CommonStatus.CREATED)
                .withParams(Map.of())
                .build();
    }

    private DatabaseServer buildDatabaseServer() {
        Map<String, Object> map = new HashMap<>();
        map.put("dbVersion", "10");

        return DatabaseServer.builder()
                .withConnectionDriver("driver")
                .withServerId("driver")
                .withConnectorJarUrl("driver")
                .withEngine(DatabaseEngine.POSTGRESQL)
                .withLocation("location")
                .withPort(99)
                .withStorageSize(50L)
                .withRootUserName("rootUserName")
                .withRootPassword("rootPassword")
                .withFlavor("flavor")
                .withUseSslEnforcement(true)
                .withParams(map)
                .build();
    }
}
