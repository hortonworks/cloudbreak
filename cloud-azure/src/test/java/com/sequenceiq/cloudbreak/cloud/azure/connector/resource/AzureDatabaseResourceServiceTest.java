package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.azure.resourcemanager.postgresql.models.ServerState.DISABLED;
import static com.azure.resourcemanager.postgresql.models.ServerState.DROPPING;
import static com.azure.resourcemanager.postgresql.models.ServerState.INACCESSIBLE;
import static com.azure.resourcemanager.postgresql.models.ServerState.READY;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureResourceType.PRIVATE_DNS_ZONE_GROUP;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureResourceType.PRIVATE_ENDPOINT;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView.DB_VERSION;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.IN_PROGRESS;
import static com.sequenceiq.cloudbreak.common.database.TargetMajorVersion.VERSION14;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DATABASE_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DNS_ZONE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DNS_ZONE_GROUP_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_ENDPOINT_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static com.sequenceiq.common.api.type.ResourceType.RDS_HOSTNAME_CANARY;
import static com.sequenceiq.common.api.type.ResourceType.RDS_PORT;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.exception.AzureException;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Storage;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.ResourceGroupUsage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureSingleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureFlexibleServerPermissionValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.service.CloudResourceValidationService;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseResourceServiceTest {

    private static final String RESOURCE_GROUP_NAME = "resource group name";

    private static final String STACK_NAME = "aStack";

    private static final String RESOURCE_REFERENCE = "aReference";

    private static final String TEMPLATE = "template is gonna do some templating";

    private static final String SERVER_NAME = "serverName";

    private static final String MIGRATED_SERVER_NAME = "migratedServerName";

    private static final String NEW_PASSWORD = "newPassword";

    @Mock
    private AzureDatabaseTemplateBuilder azureDatabaseTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DatabaseStack migratedDbStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AzureClient client;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private Deployment deployment;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Mock
    private Retry retryService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AzureFlexibleServerPermissionValidator azureFlexibleServerPermissionValidator;

    @Mock
    private PollTaskFactory statusCheckFactory;

    @Mock
    private SyncPollingScheduler<ResourcesStatePollerResult> syncPollingScheduler;

    @Mock
    private CloudResourceValidationService cloudResourceValidationService;

    @InjectMocks
    private AzureDatabaseResourceService underTest;

    private static Stream<Arguments> flexibleServerStates() {
        return Stream.of(
                Arguments.of(ServerState.DISABLED, ExternalDatabaseStatus.DELETED, 128),
                Arguments.of(ServerState.READY, ExternalDatabaseStatus.STARTED, 256),
                Arguments.of(ServerState.DROPPING, ExternalDatabaseStatus.DELETE_IN_PROGRESS, 1024),
                Arguments.of(ServerState.STOPPING, ExternalDatabaseStatus.STOP_IN_PROGRESS, 2048),
                Arguments.of(ServerState.STOPPED, ExternalDatabaseStatus.STOPPED, 4096),
                Arguments.of(ServerState.STARTING, ExternalDatabaseStatus.START_IN_PROGRESS, 8192),
                Arguments.of(ServerState.UPDATING, ExternalDatabaseStatus.UPDATE_IN_PROGRESS, 16384),
                Arguments.of(ServerState.fromString("CUSTOM"), ExternalDatabaseStatus.UNKNOWN, 32768),
                Arguments.of(null, ExternalDatabaseStatus.DELETED, 65536)
        );
    }

    private static Stream<Arguments> singleServerStates() {
        return Stream.of(
                Arguments.of(DISABLED, ExternalDatabaseStatus.DELETED, 100),
                Arguments.of(READY, ExternalDatabaseStatus.STARTED, 200),
                Arguments.of(DROPPING, ExternalDatabaseStatus.DELETE_IN_PROGRESS, 300),
                Arguments.of(INACCESSIBLE, ExternalDatabaseStatus.UNKNOWN, 400),
                Arguments.of(com.azure.resourcemanager.postgresql.models.ServerState.fromString("CUSTOM"), ExternalDatabaseStatus.UNKNOWN, 500),
                Arguments.of(null, ExternalDatabaseStatus.DELETED, 600)
        );
    }

    @BeforeEach
    void initTests() {
        lenient().when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(ac.getParameter(AzureClient.class)).thenReturn(client);
    }

    @ParameterizedTest
    @MethodSource("singleServerStates")
    void testGetDatabaseServerParametersWhenSingleServer(com.azure.resourcemanager.postgresql.models.ServerState serverState,
            ExternalDatabaseStatus externalDatabaseStatus, int storageSizeGB) {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());
        AzureSingleServerClient singleServerClientMock = mock(AzureSingleServerClient.class);
        when(client.getSingleServerClient()).thenReturn(singleServerClientMock);
        com.azure.resourcemanager.postgresql.models.Server server = mock(com.azure.resourcemanager.postgresql.models.Server.class);
        when(server.userVisibleState()).thenReturn(serverState);
        StorageProfile storageProfile = mock(StorageProfile.class);
        when(storageProfile.storageMB()).thenReturn(storageSizeGB * 1024);
        when(server.storageProfile()).thenReturn(storageProfile);
        when(singleServerClientMock.getSingleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenReturn(server);
        ExternalDatabaseParameters actual = underTest.getExternalDatabaseParameters(ac, databaseStack);
        assertEquals(externalDatabaseStatus, actual.externalDatabaseStatus());
        assertEquals(storageSizeGB * 1024L, actual.storageSizeInMB());
        assertEquals(AzureDatabaseType.SINGLE_SERVER, actual.databaseType());
    }

    @ParameterizedTest
    @MethodSource("flexibleServerStates")
    void testGetDatabaseServerParametersWhenFlexibleServer(ServerState serverState, ExternalDatabaseStatus externalDatabaseStatus, int storageSizeGB) {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        Server server = mock(Server.class);
        when(server.state()).thenReturn(serverState);
        Storage storage = mock(Storage.class);
        when(storage.storageSizeGB()).thenReturn(storageSizeGB);
        when(server.storage()).thenReturn(storage);
        when(flexibleServerClientMock.getFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenReturn(server);
        ExternalDatabaseParameters actual = underTest.getExternalDatabaseParameters(ac, databaseStack);
        assertEquals(externalDatabaseStatus, actual.externalDatabaseStatus());
        assertEquals(storageSizeGB * 1024L, actual.storageSizeInMB());
        assertEquals(AzureDatabaseType.FLEXIBLE_SERVER, actual.databaseType());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void launchCanaryDatabaseForUpgradeShouldLaunchWhenSingleServer(boolean hasException) {

        CloudContext cloudContext = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, migratedDbStack)).thenReturn(RESOURCE_GROUP_NAME);
        AzureSingleServerClient singleServerClientMock = mock(AzureSingleServerClient.class);
        when(client.getSingleServerClient()).thenReturn(singleServerClientMock);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(deployment);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(DELETED);
        if (hasException) {
            when(client.createTemplateDeployment(any(), any(), any(), any())).thenThrow(new CloudConnectorException("Error in provisioning database stack"));
        }

        com.azure.resourcemanager.postgresql.models.Server server = mock(com.azure.resourcemanager.postgresql.models.Server.class);
        when(singleServerClientMock.getSingleServer(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(server);
        when(server.userVisibleState()).thenReturn(DISABLED);

        lenient().when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));

        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get())
                .when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));

        AzureDatabaseServerView azureDatabaseServerView = mock(AzureDatabaseServerView.class);
        lenient().when(azureDatabaseServerView.getAzureDatabaseType()).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());

        DatabaseServer migratedDbServer = DatabaseServer.builder()
                .withServerId(MIGRATED_SERVER_NAME).build();

        when(migratedDbStack.getDatabaseServer()).thenReturn(migratedDbServer);
        String template = "template";
        when(azureDatabaseTemplateBuilder.build(cloudContext, migratedDbStack)).thenReturn(template);
        List<CloudResource> resources = new ArrayList<>();
        resources.add(buildResource(AZURE_DATABASE_CANARY));
        resources.add(buildResource(AZURE_DNS_ZONE_GROUP_CANARY));
        resources.add(buildResource(AZURE_PRIVATE_ENDPOINT_CANARY));
        when(azureCloudResourceService.getDeploymentCloudResources(any())).thenReturn(resources);

        if (hasException) {
            try {
                underTest.launchCanaryDatabaseForUpgrade(ac, databaseStack, migratedDbStack, persistenceNotifier);
            } catch (CloudConnectorException e) {
                verify(azureCloudResourceService).getPrivateEndpointRdsResourceTypes(true);
                verify(persistenceNotifier, times(3)).notifyDeletion(any(), eq(cloudContext));
            }
        } else {
            List<CloudResourceStatus> result = underTest.launchCanaryDatabaseForUpgrade(ac, databaseStack, migratedDbStack, persistenceNotifier);

            verify(azureCloudResourceService, never()).getPrivateEndpointRdsResourceTypes(true);
            verify(persistenceNotifier, never()).notifyDeletion(any(), any());
            assertEquals(5, result.size());
            assertTrue(result.stream().anyMatch(r -> r.getCloudResource().getType().equals(AZURE_DATABASE_CANARY)));
            assertTrue(result.stream().anyMatch(r -> r.getCloudResource().getType().equals(AZURE_DNS_ZONE_GROUP_CANARY)));
            assertTrue(result.stream().anyMatch(r -> r.getCloudResource().getType().equals(AZURE_PRIVATE_ENDPOINT_CANARY)));
            assertTrue(result.stream().anyMatch(r -> r.getCloudResource().getType().equals(RDS_PORT)));
            assertTrue(result.stream().anyMatch(r -> r.getCloudResource().getType().equals(RDS_HOSTNAME_CANARY)));
            verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, migratedDbStack);
        }
    }

    @Test
    void launchCanaryDatabaseForUpgradeShouldLaunchWhenSingleServerWithNullDeployment() {

        CloudContext cloudContext = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, migratedDbStack)).thenReturn(RESOURCE_GROUP_NAME);
        AzureSingleServerClient singleServerClientMock = mock(AzureSingleServerClient.class);
        when(client.getSingleServerClient()).thenReturn(singleServerClientMock);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(deployment);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(DELETED);
        com.azure.resourcemanager.postgresql.models.Server server = mock(com.azure.resourcemanager.postgresql.models.Server.class);
        when(singleServerClientMock.getSingleServer(RESOURCE_GROUP_NAME, MIGRATED_SERVER_NAME)).thenReturn(server);
        when(server.userVisibleState()).thenReturn(DISABLED);

        lenient().when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));

        doThrow(new Retry.ActionFailedException())
                .when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));

        AzureDatabaseServerView azureDatabaseServerView = mock(AzureDatabaseServerView.class);
        lenient().when(azureDatabaseServerView.getAzureDatabaseType()).thenReturn(AzureDatabaseType.SINGLE_SERVER);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());

        DatabaseServer migratedDbServer = DatabaseServer.builder()
                .withServerId(MIGRATED_SERVER_NAME).build();

        when(migratedDbStack.getDatabaseServer()).thenReturn(migratedDbServer);
        String template = "template";
        when(azureDatabaseTemplateBuilder.build(cloudContext, migratedDbStack)).thenReturn(template);
        List<CloudResource> resources = new ArrayList<>();
        resources.add(buildResource(AZURE_DATABASE_CANARY));
        resources.add(buildResource(AZURE_DNS_ZONE_GROUP_CANARY));
        resources.add(buildResource(AZURE_PRIVATE_ENDPOINT_CANARY));
        when(azureCloudResourceService.getDeploymentCloudResources(any())).thenReturn(resources);

        try {
            underTest.launchCanaryDatabaseForUpgrade(ac, databaseStack, migratedDbStack, persistenceNotifier);
        } catch (Retry.ActionFailedException e) {
            verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, migratedDbStack);
            verify(azureCloudResourceService).getPrivateEndpointRdsResourceTypes(true);
            verify(persistenceNotifier, times(1)).notifyDeletion(any(), eq(cloudContext));
        }
    }

    @Test
    void launchCanaryDatabaseForUpgradeShouldSkipWhenNotSingleServer() {
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());

        List<CloudResourceStatus> result = underTest.launchCanaryDatabaseForUpgrade(ac, databaseStack, migratedDbStack, persistenceNotifier);

        assertTrue(result.isEmpty());
        verify(azureCloudResourceService, never()).getPrivateEndpointRdsResourceTypes(true);
        verify(persistenceNotifier, never()).notifyDeletion(any(), any());    }

    @Test
    void testGetDatabaseServerStatusWhenException() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        RuntimeException exception = new RuntimeException("ex");
        when(flexibleServerClientMock.getFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenThrow(exception);
        CloudConnectorException actualException = assertThrows(CloudConnectorException.class, () -> underTest.getDatabaseServerStatus(ac, databaseStack));
        assertEquals(exception, actualException.getCause());
    }

    @Test
    void shouldReturnDeletedDbServerWhenTerminateDatabaseServerAndSingleResourceGroup() {
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureUtils.deleteDatabaseServer(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));

        List<CloudResourceStatus> resourceStatuses = underTest.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_DATABASE, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteDatabaseServer(any(), eq(RESOURCE_REFERENCE), anyBoolean());
        verify(client, never()).deleteResourceGroup(anyString());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldReturnDeletedResourceGroupWhenTerminateDatabaseServerAndMultipleResourceGroups() {
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.MULTIPLE);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.deleteResourceGroup(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));

        List<CloudResourceStatus> resourceStatuses = underTest.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_RESOURCE_GROUP, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteResourceGroup(any(), eq(RESOURCE_GROUP_NAME), eq(false));
        verify(azureUtils, never()).deleteDatabaseServer(any(), anyString(), anyBoolean());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldReturnDeletedDbServerAndDeleteAccessPolicyWhenTerminateDatabaseServerAndSingleResourceGroup() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyVaultUrl", "dummyKeyVaultUrl");
        params.put("keyVaultResourceGroupName", "dummyKeyVaultResourceGroupName");
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withParams(params).build());
        when(client.getServicePrincipalForResourceById(RESOURCE_REFERENCE)).thenReturn("dummyPrincipalId");
        when(client.getVaultNameFromEncryptionKeyUrl("dummyKeyVaultUrl")).thenReturn("dummyVaultName");
        when(client.keyVaultExists("dummyKeyVaultResourceGroupName", "dummyVaultName")).thenReturn(Boolean.TRUE);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureUtils.deleteDatabaseServer(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));
        initRetry();

        List<CloudResourceStatus> resourceStatuses = underTest.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_DATABASE, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteDatabaseServer(any(), eq(RESOURCE_REFERENCE), anyBoolean());
        verify(client).removeKeyVaultAccessPolicyForServicePrincipal("dummyKeyVaultResourceGroupName",
                "dummyVaultName", "dummyPrincipalId");
        verify(client, never()).deleteResourceGroup(anyString());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldReturnDeletedDbServerButNotDeleteAccessPolicyWhenTerminateFlexibleDatabaseServerAndSingleResourceGroup() {
        Map<String, Object> params = new HashMap<>();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        params.put("keyVaultUrl", "dummyKeyVaultUrl");
        params.put("keyVaultResourceGroupName", "dummyKeyVaultResourceGroupName");
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withParams(params).build());
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(any(DatabaseStack.class))).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureUtils.deleteDatabaseServer(any(), anyString(), anyBoolean())).thenReturn(Optional.empty());
        List<CloudResource> cloudResources = List.of(buildResource(AZURE_DATABASE));

        List<CloudResourceStatus> resourceStatuses = underTest.terminateDatabaseServer(ac, databaseStack, cloudResources, false, persistenceNotifier);

        assertEquals(1, resourceStatuses.size());
        assertEquals(AZURE_DATABASE, resourceStatuses.get(0).getCloudResource().getType());
        assertEquals(DELETED, resourceStatuses.get(0).getStatus());
        verify(azureUtils).deleteDatabaseServer(any(), eq(RESOURCE_REFERENCE), anyBoolean());
        verify(client, never()).removeKeyVaultAccessPolicyForServicePrincipal("dummyKeyVaultResourceGroupName",
                "dummyVaultName", "dummyPrincipalId");
        verify(client, never()).deleteResourceGroup(anyString());
        verify(persistenceNotifier).notifyDeletion(any(), any());
    }

    @Test
    void shouldUpgradeDatabaseWhenUpgradeDatabaseServerAndPrivateEndpoint() {
        DatabaseServer databaseServer = buildDatabaseServer(SINGLE_SERVER);

        CloudResource dbResource = buildResource(AZURE_DATABASE);
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP);
        List<CloudResource> cloudResourceList = List.of(peResource, dzgResource, dbResource);
        DatabaseServer originalDatabaseServer = buildDatabaseServer(SINGLE_SERVER);
        DatabaseStack originalDatabaseStack = mock(DatabaseStack.class);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(cloudResourceList);
        when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(originalDatabaseStack.getDatabaseServer()).thenReturn(originalDatabaseServer);

        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), databaseStackArgumentCaptor.capture())).thenReturn(TEMPLATE);

        underTest.upgradeDatabaseServer(ac, originalDatabaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList);

        verify(azureUtils).getStackName(eq(cloudContext));

        InOrder inOrder = inOrder(azureUtils);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_DNS_ZONE_GROUP);
        inOrder.verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        inOrder = inOrder(persistenceNotifier);
        inOrder.verify(persistenceNotifier).notifyDeletion(peResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dzgResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dbResource, cloudContext);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        assertEquals("14", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier).notifyAllocations(cloudResourceList, cloudContext);
        verify(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
    }

    @Test
    void testUpgradeThrowsMgmtExWithConflict() {
        DatabaseServer databaseServer = buildDatabaseServer(SINGLE_SERVER);

        CloudResource dbResource = buildResource(AZURE_DATABASE);
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP);
        List<CloudResource> cloudResourceList = List.of(peResource, dzgResource, dbResource);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(cloudResourceList);
        when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), databaseStackArgumentCaptor.capture())).thenReturn(TEMPLATE);
        ManagementException managementException = new ManagementException("asdf", mock(HttpResponse.class), new ManagementError("conflict", "asdf"));
        doThrow(managementException).when(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        when(azureExceptionHandler.isExceptionCodeConflict(managementException)).thenReturn(Boolean.TRUE);
        when(azureUtils.convertToCloudConnectorException(managementException, "Database stack upgrade")).thenReturn(new CloudConnectorException("fda"));

        assertThrows(CloudConnectorException.class,
                () -> underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList));

        verify(azureUtils).getStackName(eq(cloudContext));

        InOrder inOrder = inOrder(azureUtils);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_DNS_ZONE_GROUP);
        inOrder.verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        inOrder = inOrder(persistenceNotifier);
        inOrder.verify(persistenceNotifier).notifyDeletion(peResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dzgResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dbResource, cloudContext);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        assertEquals("14", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier).notifyAllocations(cloudResourceList, cloudContext);
    }

    @Test
    void testUpgradeThrowsMgmtExWithNonConflict() {
        DatabaseServer databaseServer = buildDatabaseServer(SINGLE_SERVER);

        CloudResource dbResource = buildResource(AZURE_DATABASE);
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP);
        List<CloudResource> cloudResourceList = List.of(peResource, dzgResource, dbResource);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(cloudResourceList);
        when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        when(azureDatabaseTemplateBuilder.build(eq(cloudContext), databaseStackArgumentCaptor.capture())).thenReturn(TEMPLATE);
        ManagementException managementException = new ManagementException("asdf", mock(HttpResponse.class), new ManagementError("not_conflict", "asdf"));
        doThrow(managementException).when(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        when(azureExceptionHandler.isExceptionCodeConflict(managementException)).thenReturn(Boolean.FALSE);
        when(azureUtils.convertToCloudConnectorException(managementException, "Database stack upgrade")).thenReturn(new CloudConnectorException("fda"));

        assertThrows(CloudConnectorException.class,
                () -> underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList));

        verify(azureUtils).getStackName(eq(cloudContext));

        InOrder inOrder = inOrder(azureUtils);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_DNS_ZONE_GROUP);
        inOrder.verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        inOrder = inOrder(persistenceNotifier);
        inOrder.verify(persistenceNotifier).notifyDeletion(peResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dzgResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dbResource, cloudContext);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        assertEquals("14", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier).notifyAllocations(cloudResourceList, cloudContext);
        verify(azureUtils, never()).convertToActionFailedExceptionCausedByCloudConnectorException(managementException, "Database server deployment");
    }

    @Test
    void shouldUpgradeDatabaseAndDeleteAllResourcesWhenUpgradeDatabaseServerAndMultiplePrivateEndpointResourcesExist() {
        DatabaseServer databaseServer = buildDatabaseServer(SINGLE_SERVER);

        CloudResource dbResource = buildResource(AZURE_DATABASE);
        CloudResource pe1Resource = buildResource(AZURE_PRIVATE_ENDPOINT, "pe1", CommonStatus.DETACHED);
        CloudResource pe2Resource = buildResource(AZURE_PRIVATE_ENDPOINT, "pe2", CommonStatus.FAILED);
        CloudResource pe3Resource = buildResource(AZURE_PRIVATE_ENDPOINT, "pe3", CommonStatus.CREATED);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP);
        List<CloudResource> cloudResourceList = List.of(pe1Resource, pe2Resource, pe3Resource, dzgResource, dbResource);
        List<CloudResource> expectedCloudResourceList = List.of(pe3Resource, dzgResource, dbResource);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(expectedCloudResourceList);
        when(azureCloudResourceService.getPrivateEndpointRdsResourceTypes(false)).thenReturn(List.of(AZURE_PRIVATE_ENDPOINT, AZURE_DNS_ZONE_GROUP));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList);

        verify(azureUtils).getStackName(eq(cloudContext));

        InOrder inOrder = inOrder(azureUtils);
        inOrder.verify(azureUtils, times(3)).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);
        inOrder.verify(azureUtils).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_DNS_ZONE_GROUP);
        inOrder.verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        inOrder = inOrder(persistenceNotifier);
        inOrder.verify(persistenceNotifier).notifyDeletion(pe1Resource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(pe2Resource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(pe3Resource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dzgResource, cloudContext);
        inOrder.verify(persistenceNotifier).notifyDeletion(dbResource, cloudContext);

        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureDatabaseTemplateBuilder).build(eq(cloudContext), databaseStackArgumentCaptor.capture());
        assertEquals("14", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier).notifyAllocations(expectedCloudResourceList, cloudContext);
    }

    // Correctly upgrades a Flexible Server when the type is FLEXIBLE_SERVER
    @Test
    public void testUpgradeFlexibleServerSuccessfully() {
        CloudResource dbResource = buildResource(AZURE_DATABASE);
        List<CloudResource> cloudResourceList = List.of(dbResource);
        DatabaseServer originalDatabaseServer = buildDatabaseServer(FLEXIBLE_SERVER);
        DatabaseStack originalDatabaseStack = mock(DatabaseStack.class);
        DatabaseServer databaseServer = buildDatabaseServer(FLEXIBLE_SERVER);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION14;
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(originalDatabaseStack.getDatabaseServer()).thenReturn(originalDatabaseServer);

        underTest.upgradeDatabaseServer(ac, originalDatabaseStack, databaseStack, persistenceNotifier, targetMajorVersion, cloudResourceList);

        verify(client).getFlexibleServerClient();
        verify(flexibleServerClientMock).upgrade(RESOURCE_GROUP_NAME, "name", targetMajorVersion.getMajorVersion());
    }

    @Test
    public void testUpgradeFlexibleServerWithNoCloudResourcePresent() {
        CloudResource dbResource = buildResource(AZURE_PRIVATE_ENDPOINT);
        List<CloudResource> cloudResourceList = List.of(dbResource);
        DatabaseServer databaseServer = buildDatabaseServer(FLEXIBLE_SERVER);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION14;
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, targetMajorVersion, cloudResourceList));

        verify(flexibleServerClientMock, never()).upgrade(RESOURCE_GROUP_NAME, "name", targetMajorVersion.getMajorVersion());
        assertEquals("Azure database server cloud resource does not exist for stack, this should not happen. " +
                "Please contact Cloudera support to get this resolved.", exception.getMessage());
    }

    @Test
    public void testUpgradeFlexibleServerWithException() {
        CloudResource dbResource = buildResource(AZURE_DATABASE);
        List<CloudResource> cloudResourceList = List.of(dbResource);
        DatabaseServer databaseServer = buildDatabaseServer(FLEXIBLE_SERVER);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.VERSION14;
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        ManagementException managementException = new ManagementException("anError", mock(HttpResponse.class), new ManagementError("not_conflict", "anError"));
        doThrow(managementException).when(flexibleServerClientMock).upgrade(any(), any(), any());
        when(azureUtils.convertToCloudConnectorException(managementException, "Database stack upgrade")).thenReturn(new CloudConnectorException("anError"));

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, targetMajorVersion, cloudResourceList));

        verify(client).getFlexibleServerClient();
        verify(flexibleServerClientMock).upgrade(RESOURCE_GROUP_NAME, "name", targetMajorVersion.getMajorVersion());
        assertEquals("anError", exception.getMessage());
    }

    @Test
    void shouldUpgradeDatabaseWhenUpgradeDatabaseServerAndNoPrivateEndpoint() {
        CloudResource dbResource = buildResource(AZURE_DATABASE);
        List<CloudResource> cloudResourceList = List.of(dbResource);
        DatabaseServer databaseServer = buildDatabaseServer(FLEXIBLE_SERVER);
        DatabaseServer originalDatabaseServer = buildDatabaseServer(SINGLE_SERVER);
        DatabaseStack originalDatabaseStack = mock(DatabaseStack.class);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(dbResource));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(originalDatabaseStack.getDatabaseServer()).thenReturn(originalDatabaseServer);

        underTest.upgradeDatabaseServer(ac, originalDatabaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList);

        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureUtils, never()).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);

        verify(persistenceNotifier).notifyDeletion(dbResource, cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        ArgumentCaptor<DatabaseStack> databaseStackArgumentCaptor = ArgumentCaptor.forClass(DatabaseStack.class);
        verify(azureDatabaseTemplateBuilder).build(eq(cloudContext), databaseStackArgumentCaptor.capture());
        assertEquals("14", databaseStackArgumentCaptor.getValue().getDatabaseServer().getParameters().get(DB_VERSION));
        verify(persistenceNotifier).notifyAllocations(List.of(dbResource), cloudContext);
    }

    @Test
    void shouldReturnExceptionWhenUpgradeDatabaseServerThrowsCloudException() {
        CloudResource dbResource = buildResource(AZURE_DATABASE, CommonStatus.DETACHED);
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT, CommonStatus.DETACHED);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP, CommonStatus.DETACHED);
        List<CloudResource> cloudResourceList = List.of(peResource, dzgResource, dbResource);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(dbResource));
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, SINGLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());

        doThrow(new RuntimeException("delete failed")).when(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList));

        assertEquals("Error occurred in upgrading database stack aStack: delete failed", exception.getMessage());
        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureDatabaseTemplateBuilder, never()).build(eq(cloudContext), any(DatabaseStack.class));
        verify(persistenceNotifier, times(1)).notifyAllocations(List.of(dbResource), cloudContext);
    }

    @Test
    void shouldNotReturnExceptionWhenUpgradeDatabaseServerDbResourceIsNotFound() {
        CloudResource peResource = buildResource(AZURE_PRIVATE_ENDPOINT);
        CloudResource dzgResource = buildResource(AZURE_DNS_ZONE_GROUP);
        DatabaseServer databaseServer = buildDatabaseServer(SINGLE_SERVER);

        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        List<CloudResource> cloudResourceList = List.of(peResource, dzgResource);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(List.of(mock(CloudResource.class)));
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);

        underTest.upgradeDatabaseServer(ac, databaseStack, databaseStack, persistenceNotifier, VERSION14, cloudResourceList);

        verify(azureUtils).getStackName(eq(cloudContext));
        verify(azureUtils, never()).deleteDatabaseServer(client, RESOURCE_REFERENCE, false);
        verify(azureUtils, never()).deleteGenericResourceById(client, RESOURCE_REFERENCE, PRIVATE_ENDPOINT);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureDatabaseTemplateBuilder).build(eq(cloudContext), any(DatabaseStack.class));
    }

    @Test
    void testBuildDatabaseResourcesForLaunch() {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));

        List<CloudResourceStatus> actual = underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier);

        assertEquals(2, actual.size());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
        verify(client).resourceGroupExists(RESOURCE_GROUP_NAME);
        verify(persistenceNotifier, times(4)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
        verify(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        verify(client).getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchShouldThrowExceptionWhenTheRGIsExistsAndTheTypeIsSingle() {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier));

        assertEquals("Resource group with name resource group name does not exist!", exception.getMessage());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchShouldCreateRGWhenTheExistingRGTypeIsMultiple() {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.MULTIPLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(false);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region")));
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));

        List<CloudResourceStatus> actual = underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier);

        assertEquals(2, actual.size());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
        verify(client).resourceGroupExists(RESOURCE_GROUP_NAME);
        verify(client).createResourceGroup(eq(RESOURCE_GROUP_NAME), any(), any());
        verify(persistenceNotifier, times(4)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
        verify(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        verify(client).getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchWhenTheTemplateDeploymentIsAlreadyExists() throws Exception {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(IN_PROGRESS);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));
        when(syncPollingScheduler.schedule(null)).thenReturn(new ResourcesStatePollerResult(null));

        List<CloudResourceStatus> actual = underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier);

        assertEquals(2, actual.size());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
        verify(client).resourceGroupExists(RESOURCE_GROUP_NAME);
        verify(persistenceNotifier, times(4)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
        verify(client).getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchWhenTheTemplateDeploymentAlreadyExistsAndFailed() throws Exception {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(IN_PROGRESS);
        ResourcesStatePollerResult resourcesStatePollerResult = new ResourcesStatePollerResult(
                null, ResourceStatus.FAILED, "", List.of(new CloudResourceStatus(null, ResourceStatus.FAILED)));
        when(syncPollingScheduler.schedule(null)).thenReturn(resourcesStatePollerResult);
        CloudConnectorException cloudConnectorException = new CloudConnectorException("msg");
        doThrow(cloudConnectorException).when(cloudResourceValidationService).validateResourcesState(cloudContext, resourcesStatePollerResult);

        CloudConnectorException actualException = assertThrows(CloudConnectorException.class,
                () -> underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier));

        assertEquals(cloudConnectorException, actualException);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchWhenTheTemplateDeploymentAlreadyExistsAndException() throws Exception {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(IN_PROGRESS);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));
        doThrow(new Exception("msg")).when(syncPollingScheduler).schedule(null);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier));
        assertEquals("msg", exception.getMessage());
    }

    @Test
    void testBuildDatabaseResourcesForLaunchShouldThrowExceptionWhenTheTemplateDeploymentThrowsManagementException() {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));
        ManagementException managementException = new ManagementException("Error", mock(HttpResponse.class));
        doThrow(managementException).when(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        String exceptionMessage = "Database stack provisioning";
        when(azureUtils.convertToCloudConnectorException(managementException, exceptionMessage))
                .thenReturn(new CloudConnectorException(exceptionMessage, managementException));
        ArrayList<CloudResource> resources = new ArrayList<>();
        resources.add(mock(CloudResource.class));
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(resources);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier));

        assertEquals(exceptionMessage, exception.getMessage());
        assertEquals(managementException, exception.getCause());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
        verify(client).resourceGroupExists(RESOURCE_GROUP_NAME);
        verify(persistenceNotifier, times(5)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
        verify(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        verify(client).getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME);
        verify(azureUtils).convertToCloudConnectorException(managementException, exceptionMessage);
        verify(azureCloudResourceService).getDeploymentCloudResources(deployment);
    }

    @Test
    void testBuildDatabaseResourcesForLaunchShouldThrowExceptionWhenTheTemplateDeploymentThrowsException() {
        when(azureUtils.getStackName(cloudContext)).thenReturn(STACK_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        when(azureResourceGroupMetadataProvider.getResourceGroupUsage(databaseStack)).thenReturn(ResourceGroupUsage.SINGLE);
        when(azureDatabaseTemplateBuilder.build(cloudContext, databaseStack)).thenReturn(TEMPLATE);
        when(client.resourceGroupExists(RESOURCE_GROUP_NAME)).thenReturn(true);
        when(client.getTemplateDeploymentStatus(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(DELETED);
        when(client.getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME)).thenReturn(deployment);
        when(deployment.outputs()).thenReturn(Map.of("databaseServerFQDN", Map.of("value", "fqdn")));
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(retryService).testWith2SecDelayMax5Times(any(Runnable.class));
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(retryService).testWith2SecDelayMax5Times(any(Supplier.class));
        AzureException azureException = new AzureException("Error");
        doThrow(azureException).when(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        ArrayList<CloudResource> resources = new ArrayList<>();
        resources.add(mock(CloudResource.class));
        when(azureCloudResourceService.getDeploymentCloudResources(deployment)).thenReturn(resources);

        Exception exception = assertThrows(CloudConnectorException.class,
                () -> underTest.buildDatabaseResourcesForLaunch(ac, databaseStack, persistenceNotifier));

        assertEquals("Error in provisioning database stack aStack: Error", exception.getMessage());
        assertEquals(azureException, exception.getCause());
        verify(azureUtils).getStackName(cloudContext);
        verify(azureResourceGroupMetadataProvider).getResourceGroupName(cloudContext, databaseStack);
        verify(azureResourceGroupMetadataProvider).getResourceGroupUsage(databaseStack);
        verify(azureDatabaseTemplateBuilder).build(cloudContext, databaseStack);
        verify(client).resourceGroupExists(RESOURCE_GROUP_NAME);
        verify(persistenceNotifier, times(5)).notifyAllocation(any(CloudResource.class), eq(cloudContext));
        verify(client).createTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME, TEMPLATE, "{}");
        verify(client).getTemplateDeployment(RESOURCE_GROUP_NAME, STACK_NAME);
        verify(azureCloudResourceService).getDeploymentCloudResources(deployment);
    }

    @Test
    void updateDefaultSingleServerAdministratorLoginPasswordShouldSucceed() {
        AzureSingleServerClient azureSingleServerClient = mock(AzureSingleServerClient.class);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());
        when(azureResourceGroupMetadataProvider.getResourceGroupName(eq(cloudContext), eq(databaseStack))).thenReturn(RESOURCE_GROUP_NAME);
        when(client.getSingleServerClient()).thenReturn(azureSingleServerClient);
        underTest.updateAdministratorLoginPassword(ac, databaseStack, NEW_PASSWORD);

        verify(azureResourceGroupMetadataProvider, times(1)).getResourceGroupName(eq(cloudContext), eq(databaseStack));
        verify(azureSingleServerClient, times(1)).updateAdministratorLoginPassword(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME), eq(NEW_PASSWORD));
    }

    @Test
    void updateDefaultSingleServerAdministratorLoginPasswordShouldFailWhenClientThrowsException() {
        AzureSingleServerClient azureSingleServerClient = mock(AzureSingleServerClient.class);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());
        when(azureResourceGroupMetadataProvider.getResourceGroupName(eq(cloudContext), eq(databaseStack))).thenReturn(RESOURCE_GROUP_NAME);
        when(client.getSingleServerClient()).thenReturn(azureSingleServerClient);
        doThrow(new RuntimeException("error")).when(azureSingleServerClient)
                .updateAdministratorLoginPassword(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME), eq(NEW_PASSWORD));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.updateAdministratorLoginPassword(ac, databaseStack, NEW_PASSWORD));

        assertEquals("error", cloudConnectorException.getMessage());
        verify(azureResourceGroupMetadataProvider, times(1)).getResourceGroupName(eq(cloudContext), eq(databaseStack));
        verify(azureSingleServerClient, times(1)).updateAdministratorLoginPassword(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME), eq(NEW_PASSWORD));
    }

    @Test
    void updateFlexibleServerAdministratorLoginPasswordShouldSucceed() {
        AzureFlexibleServerClient azureFlexibleServerClient = mock(AzureFlexibleServerClient.class);
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME)
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER)).build());
        when(azureResourceGroupMetadataProvider.getResourceGroupName(eq(cloudContext), eq(databaseStack))).thenReturn(RESOURCE_GROUP_NAME);
        when(client.getFlexibleServerClient()).thenReturn(azureFlexibleServerClient);
        underTest.updateAdministratorLoginPassword(ac, databaseStack, NEW_PASSWORD);

        verify(azureResourceGroupMetadataProvider, times(1)).getResourceGroupName(eq(cloudContext), eq(databaseStack));
        verify(azureFlexibleServerClient, times(1)).updateAdministratorLoginPassword(eq(RESOURCE_GROUP_NAME), eq(SERVER_NAME), eq(NEW_PASSWORD));
    }

    @Test
    void testStartDatabaseServerFlexible() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        underTest.startDatabaseServer(ac, databaseStack);
        verify(flexibleServerClientMock, times(1)).startFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME);
    }

    @Test
    void testStartDatabaseServerSingle() {
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().build());
        underTest.startDatabaseServer(ac, databaseStack);
        verify(client, never()).getFlexibleServerClient();
    }

    @Test
    void testStopDatabaseServerFlexible() {
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClientMock = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClientMock);
        underTest.stopDatabaseServer(ac, databaseStack);
        verify(flexibleServerClientMock, times(1)).stopFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME);
    }

    @Test
    void testStopDatabaseServerSingle() {
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().build());
        underTest.stopDatabaseServer(ac, databaseStack);
        verify(client, never()).getFlexibleServerClient();
    }

    private CloudResource buildResource(ResourceType resourceType) {
        return buildResource(resourceType, CommonStatus.CREATED);
    }

    private CloudResource buildResource(ResourceType resourceType, CommonStatus status) {
        return buildResource(resourceType, "name", status);
    }

    private CloudResource buildResource(ResourceType resourceType, String name, CommonStatus status) {
        return CloudResource.builder()
                .withType(resourceType)
                .withReference(RESOURCE_REFERENCE)
                .withName(name)
                .withStatus(status)
                .withParameters(Map.of())
                .build();
    }

    private DatabaseServer buildDatabaseServer(AzureDatabaseType databaseType) {
        Map<String, Object> map = new HashMap<>();
        map.put("dbVersion", "10");
        map.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, databaseType.name());

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

    private void initRetry() {
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }
}