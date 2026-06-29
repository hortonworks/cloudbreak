package com.sequenceiq.cloudbreak.cm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerKraftMigrationServiceTest {

    private static final String STACK_NAME = "stack_name";

    private static final String KAFKA_SERVICE_NAME = "KAFKA";

    private static final String ZOOKEEPER_SERVICE_NAME = "ZOOKEEPER";

    private static final long COMMAND_ID = 1L;

    private static final String KRAFT_ROLE_TYPE = "KRAFT";

    @InjectMocks
    private ClouderaManagerKraftMigrationService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerConfigService configService;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private RolesResourceApi rolesResourceApi;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Mock
    private RoleConfigGroupsResourceApi roleConfigGroupsResourceApi;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setName(STACK_NAME);
        Cluster cluster = new Cluster();
        cluster.setName(STACK_NAME);
        stack.setCluster(cluster);

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        lenient().when(clouderaManagerApiFactory.getRolesResourceApi(apiClient)).thenReturn(rolesResourceApi);
    }

    private void stubKafkaAndZookeeperServices(String kafkaServiceInstance, String zookeeperServiceInstance) {
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.of(kafkaServiceInstance));
        when(configService.getServiceName(eq(STACK_NAME), eq(ZOOKEEPER_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.of(zookeeperServiceInstance));
    }

    @Test
    void testConfigureZookeeperToKraftMigration() throws ApiException {
        String roleConfigGroupName = "kraft-KRAFT-BASE";
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient)).thenReturn(roleConfigGroupsResourceApi);
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.of(KAFKA_SERVICE_NAME));
        when(configService.getRoleConfigGroupNameByTypeAndServiceName(eq(KRAFT_ROLE_TYPE), eq(STACK_NAME),
                eq(KAFKA_SERVICE_NAME), eq(roleConfigGroupsResourceApi)))
                .thenReturn(roleConfigGroupName);

        underTest.configureZookeeperToKraftMigration(apiClient, stack);

        verify(configService).modifyRoleConfigGroup(eq(apiClient), eq(STACK_NAME), eq(KAFKA_SERVICE_NAME),
                eq(roleConfigGroupName), eq(Map.of("kraft.properties_role_safety_valve", "zookeeper.metadata.migration.enable=true\n" +
                        "log.dirs=/hadoopfs/fs1/kraft", "metadata.log.dir", "/hadoopfs/fs1/kraft")));
    }

    @Test
    void testConfigureZookeeperToKraftMigrationWhenApiExceptionOccurs() throws ApiException {
        String roleConfigGroupName = "kraft-KRAFT-BASE";
        String errorMessage = "Error retrieving role config group name";
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient)).thenReturn(roleConfigGroupsResourceApi);
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.of(KAFKA_SERVICE_NAME));
        when(configService.getRoleConfigGroupNameByTypeAndServiceName(eq(KRAFT_ROLE_TYPE), eq(STACK_NAME),
                eq(KAFKA_SERVICE_NAME), eq(roleConfigGroupsResourceApi)))
                .thenThrow(new ApiException(errorMessage));

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.configureZookeeperToKraftMigration(apiClient, stack));
        assertEquals(errorMessage, expectedException.getMessage());

        verify(configService, times(0)).modifyRoleConfigGroup(eq(apiClient), eq(STACK_NAME), eq(KAFKA_SERVICE_NAME),
                eq(roleConfigGroupName), any());
    }

    @Test
    void testConfigureZookeeperToKraftMigrationWhenKafkaServiceNotFound() {
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient)).thenReturn(roleConfigGroupsResourceApi);
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.empty());

        assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.configureZookeeperToKraftMigration(apiClient, stack));
    }

    @Test
    void testMigrateZookeeperToKraft() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        underTest.migrateZookeeperToKraft(apiClient, stack);

        verify(servicesResourceApi).serviceCommandByName(eq(STACK_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID));
    }

    @Test
    void testMigrateZookeeperToKraftWhenKafkaServiceNotFound() throws ApiException {
        ApiServiceList serviceList = new ApiServiceList().items(List.of());

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);

        underTest.migrateZookeeperToKraft(apiClient, stack);

        verify(servicesResourceApi).readServices(eq(STACK_NAME), any());
    }

    @Test
    void testMigrateZookeeperToKraftWhenCommandIsAlreadyRunning() throws ApiException, CloudbreakException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand previousMigrationCommand = new ApiCommand().id(COMMAND_ID).active(true).success(false).canRetry(true);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(syncApiCommandRetriever.getCommandId("KRaftMigrationCommand", clustersResourceApi, stack)).thenReturn(Optional.of(COMMAND_ID));
        when(clouderaManagerCommandsService.getApiCommand(apiClient, COMMAND_ID)).thenReturn(previousMigrationCommand);

        underTest.migrateZookeeperToKraft(apiClient, stack);

        verifyNoMoreInteractions(clouderaManagerCommandsService);
        verifyNoMoreInteractions(clouderaManagerPollingServiceProvider);
    }

    @Test
    void testMigrateZookeeperToKraftWhenPollingTimeout() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.migrateZookeeperToKraft(apiClient, stack));
        assertEquals("Timeout during waiting for command API to be available (Zookeeper to KRaft migration)",
                expectedException.getMessage());
    }

    @Test
    void testFinalizeZookeeperToKraftMigration() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftFinalizeMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingFinalizeZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        underTest.finalizeZookeeperToKraftMigration(apiClient, stack);

        verify(servicesResourceApi).serviceCommandByName(eq(STACK_NAME), eq("KRaftFinalizeMigrationCommand"),
                eq(KAFKA_SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingFinalizeZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID));
    }

    @Test
    void testFinalizeZookeeperToKraftMigrationWhenPollingTimeout() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftFinalizeMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingFinalizeZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.finalizeZookeeperToKraftMigration(apiClient, stack));
        assertEquals("Timeout during waiting for command API to be available (Zookeeper to KRaft migration finalization)",
                expectedException.getMessage());
    }

    @Test
    void testRollbackZookeeperToKraftMigration() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftRollbackMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingRollbackZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        underTest.rollbackZookeeperToKraftMigration(apiClient, stack);

        verify(servicesResourceApi).serviceCommandByName(eq(STACK_NAME), eq("KRaftRollbackMigrationCommand"),
                eq(KAFKA_SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingRollbackZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID));
    }

    @Test
    void testRollbackZookeeperToKraftMigrationWhenPollingTimeout() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_NAME);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .build();

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(STACK_NAME), eq("KRaftRollbackMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingRollbackZookeeperToKraftMigration(
                eq(stack), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.rollbackZookeeperToKraftMigration(apiClient, stack));
        assertEquals("Timeout during waiting for command API to be available (Zookeeper to KRaft migration rollback)",
                expectedException.getMessage());
    }

    @Test
    @DisplayName("installKraftAsStopped should create KRaft role per Zookeeper host")
    void installKraftAsStoppedCreatesKraftRolePerZookeeperHost() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        ApiHostRef h2 = new ApiHostRef().hostname("host-2.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().hostRef(h1),
                        new ApiRole().hostRef(h2)
                )));
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("kafka-1"), eq("type==KRAFT"), anyString()))
                .thenReturn(new ApiRoleList().items(List.of()));

        underTest.installKraftAsStopped(apiClient, stack);

        ArgumentCaptor<ApiRoleList> roleListCaptor = ArgumentCaptor.forClass(ApiRoleList.class);
        verify(rolesResourceApi).createRoles(eq(STACK_NAME), eq("kafka-1"), roleListCaptor.capture());

        ApiRoleList created = roleListCaptor.getValue();
        assertThat(created).isNotNull();
        assertThat(created.getItems()).hasSize(2);

        ApiRole r1 = created.getItems().get(0);
        assertEquals(KRAFT_ROLE_TYPE, r1.getType());
        assertEquals(ApiRoleState.STOPPED, r1.getRoleState());
        assertEquals("host-1.example", r1.getHostRef().getHostname());
        assertEquals(STACK_NAME, r1.getServiceRef().getClusterName());
        assertEquals("kafka-1", r1.getServiceRef().getServiceName());

        ApiRole r2 = created.getItems().get(1);
        assertEquals(KRAFT_ROLE_TYPE, r2.getType());
        assertEquals(ApiRoleState.STOPPED, r2.getRoleState());
        assertEquals("host-2.example", r2.getHostRef().getHostname());
        assertEquals(STACK_NAME, r2.getServiceRef().getClusterName());
        assertEquals("kafka-1", r2.getServiceRef().getServiceName());
    }

    @Test
    @DisplayName("installKraftAsStopped should fail when Kafka service is missing")
    void installKraftAsStoppedWhenKafkaMissing() {
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.empty());

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(apiClient, stack));

        assertThat(ex.getCause()).isInstanceOf(ClouderaManagerOperationFailedException.class);
        assertEquals("Service of type: KAFKA is not found", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("installKraftAsStopped should fail when Zookeeper service is missing")
    void installKraftAsStoppedZookeeperMissing() {
        when(configService.getServiceName(eq(STACK_NAME), eq(KAFKA_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.of("kafka-1"));
        when(configService.getServiceName(eq(STACK_NAME), eq(ZOOKEEPER_SERVICE_NAME), eq(servicesResourceApi)))
                .thenReturn(Optional.empty());

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(apiClient, stack));

        assertThat(ex.getCause()).isInstanceOf(ClouderaManagerOperationFailedException.class);
        assertEquals("Service of type: ZOOKEEPER is not found", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("installKraftAsStopped should not create KRaft role when no Zookeeper hosts are present")
    void installKraftAsStoppedNoZookeeperHosts() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString()))
                .thenReturn(new ApiRoleList().items(List.of()));

        underTest.installKraftAsStopped(apiClient, stack);

        verify(rolesResourceApi, never()).readRoles(eq(STACK_NAME), eq("kafka-1"), eq("type==KRAFT"), anyString());
        verify(rolesResourceApi, never()).createRoles(anyString(), anyString(), any(ApiRoleList.class));
    }

    @Test
    @DisplayName("installKraftAsStopped should skip KRaft role creation when roles already exist on all Zookeeper hosts")
    void installKraftAsStoppedSkipsWhenKraftRolesAlreadyExist() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        ApiHostRef h2 = new ApiHostRef().hostname("host-2.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().hostRef(h1),
                        new ApiRole().hostRef(h2)
                )));
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("kafka-1"), eq("type==KRAFT"), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().type(KRAFT_ROLE_TYPE).hostRef(h1),
                        new ApiRole().type(KRAFT_ROLE_TYPE).hostRef(h2)
                )));

        underTest.installKraftAsStopped(apiClient, stack);

        verify(rolesResourceApi, never()).createRoles(anyString(), anyString(), any(ApiRoleList.class));
    }

    @Test
    @DisplayName("installKraftAsStopped should create KRaft role only on hosts missing an existing KRaft role")
    void installKraftAsStoppedCreatesOnlyMissingKraftRoles() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        ApiHostRef h2 = new ApiHostRef().hostname("host-2.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().hostRef(h1),
                        new ApiRole().hostRef(h2)
                )));
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("kafka-1"), eq("type==KRAFT"), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().type(KRAFT_ROLE_TYPE).hostRef(h1)
                )));

        underTest.installKraftAsStopped(apiClient, stack);

        ArgumentCaptor<ApiRoleList> roleListCaptor = ArgumentCaptor.forClass(ApiRoleList.class);
        verify(rolesResourceApi).createRoles(eq(STACK_NAME), eq("kafka-1"), roleListCaptor.capture());
        assertThat(roleListCaptor.getValue().getItems()).hasSize(1);
        assertEquals("host-2.example", roleListCaptor.getValue().getItems().get(0).getHostRef().getHostname());
    }

    @Test
    @DisplayName("installKraftAsStopped should throw CloudbreakException when readRoles throws ApiException")
    void installKraftAsStoppedReadRolesThrowsApiException() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        ApiException apiException = new ApiException("boom");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString())).thenThrow(apiException);

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(apiClient, stack));
        assertThat(ex.getCause()).isSameAs(apiException);
    }

    @Test
    @DisplayName("installKraftAsStopped should throw CloudbreakException when createRoles throws ApiException")
    void installKraftAsStoppedCreateRolesThrowsApiException() throws Exception {
        stubKafkaAndZookeeperServices("kafka-1", "zookeeper-1");

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), isNull(), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(new ApiRole().hostRef(h1))));
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("kafka-1"), eq("type==KRAFT"), anyString()))
                .thenReturn(new ApiRoleList().items(List.of()));

        ApiException apiException = new ApiException("boom");
        when(rolesResourceApi.createRoles(eq(STACK_NAME), eq("kafka-1"), any(ApiRoleList.class))).thenThrow(apiException);

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(apiClient, stack));
        assertThat(ex.getCause()).isSameAs(apiException);
    }
}
