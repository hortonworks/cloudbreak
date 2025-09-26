package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerKraftMigrationServiceTest {

    private static final String CLUSTER_NAME = "test-cluster";

    private static final String KAFKA_SERVICE_NAME = "kafka-1";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final BigDecimal COMMAND_ID = BigDecimal.ONE;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClouderaManagerConfigService configService;

    @Mock
    private ClouderaManagerRestartService clouderaManagerRestartService;

    @Mock
    private ApiClient apiClient;

    @Mock
    private StackDtoDelegate stackDtoDelegate;

    @Mock
    private ClusterView clusterView;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private RoleConfigGroupsResourceApi roleConfigGroupsResourceApi;

    @InjectMocks
    private ClouderaManagerKraftMigrationService underTest;

    @BeforeEach
    void setUp() {
        when(stackDtoDelegate.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(CLUSTER_NAME);
    }

    @Test
    void testEnableZookeeperMigrationMode() throws ApiException {
        String roleConfigGroupName = "kraft-KRAFT-BASE";
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient)).thenReturn(roleConfigGroupsResourceApi);
        when(configService.getServiceName(eq(CLUSTER_NAME), eq(KAFKA_SERVICE_TYPE), eq(servicesResourceApi)))
                .thenReturn(Optional.of(KAFKA_SERVICE_NAME));
        when(configService.getRoleConfigGroupNameByTypeAndServiceName(eq("KRAFT"), eq(CLUSTER_NAME),
                eq(KAFKA_SERVICE_NAME), eq(roleConfigGroupsResourceApi)))
                .thenReturn(roleConfigGroupName);

        underTest.enableZookeeperMigrationMode(apiClient, stackDtoDelegate);

        verify(configService).modifyRoleConfigGroup(eq(apiClient), eq(CLUSTER_NAME), eq(KAFKA_SERVICE_NAME),
                eq(roleConfigGroupName), any());
    }

    @Test
    void testEnableZookeeperMigrationModeWhenKafkaServiceNotFound() {
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient)).thenReturn(roleConfigGroupsResourceApi);
        when(configService.getServiceName(eq(CLUSTER_NAME), eq(KAFKA_SERVICE_TYPE), eq(servicesResourceApi)))
                .thenReturn(Optional.empty());

        assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.enableZookeeperMigrationMode(apiClient, stackDtoDelegate));
    }

    @Test
    void testRestartKafkaBrokerNodes() {
        underTest.restartKafkaBrokerNodes(apiClient, stackDtoDelegate);

        verify(clouderaManagerRestartService).restartServiceRolesByType(
                eq(stackDtoDelegate), eq(apiClient), eq(KAFKA_SERVICE_TYPE), eq("KAFKA_BROKER"));
    }

    @Test
    void testRestartKafkaConnectNodes() {
        underTest.restartKafkaConnectNodes(apiClient, stackDtoDelegate);

        verify(clouderaManagerRestartService).restartServiceRolesByType(
                eq(stackDtoDelegate), eq(apiClient), eq(KAFKA_SERVICE_TYPE), eq("KAFKA_CONNECT"));
    }

    @Test
    void testMigrateZookeeperToKraft() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_TYPE);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

        when(stackDtoDelegate.getName()).thenReturn(CLUSTER_NAME);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(CLUSTER_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(CLUSTER_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingZookeeperToKraftMigration(
                eq(stackDtoDelegate), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        underTest.migrateZookeeperToKraft(apiClient, stackDtoDelegate);

        verify(servicesResourceApi).serviceCommandByName(eq(CLUSTER_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingZookeeperToKraftMigration(
                eq(stackDtoDelegate), eq(apiClient), eq(COMMAND_ID));
    }

    @Test
    void testMigrateZookeeperToKraftWhenKafkaServiceNotFound() throws ApiException {
        ApiServiceList serviceList = new ApiServiceList().items(List.of());

        when(stackDtoDelegate.getName()).thenReturn(CLUSTER_NAME);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(CLUSTER_NAME), any())).thenReturn(serviceList);

        underTest.migrateZookeeperToKraft(apiClient, stackDtoDelegate);

        verify(servicesResourceApi).readServices(eq(CLUSTER_NAME), any());
    }

    @Test
    void testMigrateZookeeperToKraftWhenPollingTimeout() throws ApiException {
        ApiService kafkaService = new ApiService().name(KAFKA_SERVICE_NAME).type(KAFKA_SERVICE_TYPE);
        ApiServiceList serviceList = new ApiServiceList().items(List.of(kafkaService));
        ApiCommand migrationCommand = new ApiCommand().id(COMMAND_ID);
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .timeout()
                .build();

        when(stackDtoDelegate.getName()).thenReturn(CLUSTER_NAME);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(CLUSTER_NAME), any())).thenReturn(serviceList);
        when(servicesResourceApi.serviceCommandByName(eq(CLUSTER_NAME), eq("KRaftMigrationCommand"),
                eq(KAFKA_SERVICE_NAME))).thenReturn(migrationCommand);
        when(clouderaManagerPollingServiceProvider.startPollingZookeeperToKraftMigration(
                eq(stackDtoDelegate), eq(apiClient), eq(COMMAND_ID))).thenReturn(pollingResult);

        assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.migrateZookeeperToKraft(apiClient, stackDtoDelegate));
    }
}