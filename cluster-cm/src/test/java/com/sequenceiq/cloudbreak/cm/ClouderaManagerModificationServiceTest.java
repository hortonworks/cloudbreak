package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.CDH;
import static com.sequenceiq.cloudbreak.cm.util.TestUtil.FLINK;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_2_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigRecord;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostReallocateMemoryResponse;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.AutoConfigApplicability;
import com.google.common.collect.HashBasedTable;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.ResetJvmParamsDiff;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.TrustView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class ClouderaManagerModificationServiceTest extends ClouderaManagerModificationServiceTestBase {

    @Test
    @DisplayName("installKraftAsStopped should create KRaft role per Zookeeper host")
    void installKraftAsStoppedCreatesKraftRolePerZookeeperHost() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(List.of(
                new ApiService().type(KAFKA_SERVICE_NAME).name("kafka-1"),
                new ApiService().type(ZOOKEEPER_SERVICE_NAME).name("zookeeper-1")
        )));

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        ApiHostRef h2 = new ApiHostRef().hostname("host-2.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), eq(null), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(
                        new ApiRole().hostRef(h1),
                        new ApiRole().hostRef(h2)
                )));

        underTest.installKraftAsStopped(stack);

        ArgumentCaptor<ApiRoleList> roleListCaptor = ArgumentCaptor.forClass(ApiRoleList.class);
        verify(rolesResourceApi).createRoles(eq(STACK_NAME), eq("kafka-1"), roleListCaptor.capture());

        ApiRoleList created = roleListCaptor.getValue();
        assertThat(created).isNotNull();
        assertThat(created.getItems()).hasSize(2);

        ApiRole r1 = created.getItems().get(0);
        assertEquals("KRAFT", r1.getType());
        assertEquals(ApiRoleState.STOPPED, r1.getRoleState());
        assertEquals("host-1.example", r1.getHostRef().getHostname());
        assertEquals(STACK_NAME, r1.getServiceRef().getClusterName());
        assertEquals("kafka-1", r1.getServiceRef().getServiceName());

        ApiRole r2 = created.getItems().get(1);
        assertEquals("KRAFT", r2.getType());
        assertEquals(ApiRoleState.STOPPED, r2.getRoleState());
        assertEquals("host-2.example", r2.getHostRef().getHostname());
        assertEquals(STACK_NAME, r2.getServiceRef().getClusterName());
        assertEquals("kafka-1", r2.getServiceRef().getServiceName());
    }

    @Test
    @DisplayName("installKraftAsStopped should not create KRaft role for non-Kafka service")
    void installKraftAsStoppedWhenKafkaMissing() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString()))
                .thenReturn(new ApiServiceList().items(List.of(new ApiService().type(ZOOKEEPER_SERVICE_NAME).name("zookeeper-1"))));

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(stack));

        assertThat(ex.getCause()).isInstanceOf(ClouderaManagerOperationFailedException.class);
        assertEquals("Kafka service not found in the cluster!", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("installKraftAsStopped should not create KRaft role for missing Zookeeper service")
    void installKraftAsStoppedZookeeperMissing() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString()))
                .thenReturn(new ApiServiceList().items(List.of(new ApiService().type(KAFKA_SERVICE_NAME).name("kafka-1"))));

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(stack));

        assertThat(ex.getCause()).isInstanceOf(ClouderaManagerOperationFailedException.class);
        assertEquals("Zookeeper service not found in the cluster!", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("installKraftAsStopped should not create KRaft role for missing Kafka service")
    void installKraftAsStoppedNoZookeeperHosts() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(List.of(
                new ApiService().type(KAFKA_SERVICE_NAME).name("kafka-1"),
                new ApiService().type(ZOOKEEPER_SERVICE_NAME).name("zookeeper-1")
        )));
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), eq(null), anyString()))
                .thenReturn(new ApiRoleList().items(List.of()));

        underTest.installKraftAsStopped(stack);

        ArgumentCaptor<ApiRoleList> roleListCaptor = ArgumentCaptor.forClass(ApiRoleList.class);
        verify(rolesResourceApi).createRoles(eq(STACK_NAME), eq("kafka-1"), roleListCaptor.capture());
        assertThat(roleListCaptor.getValue().getItems()).isNull();
    }

    @Test
    @DisplayName("installKraftAsStopped should throw CloudbreakException when readServices throws ApiException")
    void installKraftAsStoppedReadServicesThrowsApiException() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        ApiException apiException = new ApiException("boom");
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenThrow(apiException);

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(stack));
        assertThat(ex.getCause()).isSameAs(apiException);
    }

    @Test
    @DisplayName("installKraftAsStopped should throw CloudbreakException when readRoles throws ApiException")
    void installKraftAsStoppedReadRolesThrowsApiException() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(List.of(
                new ApiService().type(KAFKA_SERVICE_NAME).name("kafka-1"),
                new ApiService().type(ZOOKEEPER_SERVICE_NAME).name("zookeeper-1")
        )));

        ApiException apiException = new ApiException("boom");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), eq(null), anyString())).thenThrow(apiException);

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(stack));
        assertThat(ex.getCause()).isSameAs(apiException);
    }

    @Test
    @DisplayName("installKraftAsStopped should throw CloudbreakException when createRoles throws ApiException")
    void installKraftAsStoppedCreateRolesThrowsApiException() throws Exception {
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getRolesResourceApi(v31Client)).thenReturn(rolesResourceApi);

        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(List.of(
                new ApiService().type(KAFKA_SERVICE_NAME).name("kafka-1"),
                new ApiService().type(ZOOKEEPER_SERVICE_NAME).name("zookeeper-1")
        )));

        ApiHostRef h1 = new ApiHostRef().hostname("host-1.example");
        when(rolesResourceApi.readRoles(eq(STACK_NAME), eq("zookeeper-1"), eq(null), anyString()))
                .thenReturn(new ApiRoleList().items(List.of(new ApiRole().hostRef(h1))));

        ApiException apiException = new ApiException("boom");
        when(rolesResourceApi.createRoles(eq(STACK_NAME), eq("kafka-1"), any(ApiRoleList.class))).thenThrow(apiException);

        CloudbreakException ex = assertThrows(CloudbreakException.class, () -> underTest.installKraftAsStopped(stack));
        assertThat(ex.getCause()).isSameAs(apiException);
    }

    @Test
    void testPollRefreshWhenCancelled() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(exit);
        doThrow(new CancellationException("Cluster was terminated while waiting for service refresh")).when(pollingResultErrorHandler)
                .handlePollingResult(eq(exit), any(), any());
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals("Cluster was terminated while waiting for service refresh", actual.getMessage());
    }

    @Test
    void testPollRefreshWhenTimeout() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        String expectedMessage = "Timeout while Cloudera Manager was refreshing services.";
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(timeout);
        doThrow(new CloudbreakException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(timeout), any(), any());
        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenCancelled() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        String expectedMessage = "Cluster was terminated while waiting for config deploy";
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(exit);
        doThrow(new CancellationException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(exit), any(), any());
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollDeployConfig(apiCommand.getId()));
        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenTimeout() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(timeout);
        String expectedMessage = "Timeout while Cloudera Manager was config deploying services.";
        doThrow(new CloudbreakException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(timeout), any(), any());
        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.pollDeployConfig(apiCommand.getId()));
        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenNoStaleConfig() throws CloudbreakException, ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.FRESH)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);

        verify(clouderaManagerPollingServiceProvider, times(0)).startPollingCmClientConfigDeployment(eq(stack), eq(v31Client), any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenConfigStale() throws CloudbreakException, ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        List<ApiCommand> apiCommands = List.of(
                new ApiCommand().name("DeployClusterClientConfig").id(1L),
                new ApiCommand().name("RefreshCluster").id(1L));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenReturn(new ApiCommand().id(1L));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmConfigurationRefresh(eq(stack), eq(v31Client), any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenRefreshFailAndForcedIsTrueSwallowError() throws CloudbreakException, ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        List<ApiCommand> apiCommands = List.of(
                new ApiCommand().name("DeployClusterClientConfig").id(1L),
                new ApiCommand().name("RefreshCluster").id(1L));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenThrow(new ClouderaManagerOperationFailedException("RefreshCommand failed"));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, true);

        verify(clouderaManagerClientConfigDeployService, times(1)).deployAndPollClientConfig(any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenRefreshFailAndForcedIsFalseFail() throws ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        List<ApiCommand> apiCommands = List.of(
                new ApiCommand().name("DeployClusterClientConfig").id(1L),
                new ApiCommand().name("RefreshCluster").id(1L));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenThrow(new ClouderaManagerOperationFailedException("RefreshCommand failed"));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false));

        assertEquals("RefreshCommand failed", exception.getMessage());
    }

    @Test
    public void testDeployConfigAndRestartClusterServices() throws Exception {
        // GIVEN
        doNothing().when(clouderaManagerClientConfigDeployService).deployAndPollClientConfig(any());
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
        // WHEN
        underTest.deployConfigAndRestartClusterServices(false);
        // THEN
        verify(configService, times(1)).modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), true);
    }

    @Test
    void removeUnusedParcels() {
        // GIVEN
        Set<String> parcelNamesFromImage = new HashSet<>();
        ClouderaManagerProduct cmProduct1 = createClouderaManagerProduct("product1", "version1");
        ClouderaManagerProduct cmProduct2 = createClouderaManagerProduct("product2", "version2");
        Set<ClusterComponentView> usedComponents = Set.of(createClusterComponent(cmProduct1), createClusterComponent(cmProduct2));
        Set<String> usedParcelComponentNames = Set.of(cmProduct1.getName(), cmProduct2.getName());
        when(clouderaManagerApiFactory.getParcelsResourceApi(v31Client)).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(v31Client)).thenReturn(parcelResourceApi);
        when(clouderaManagerParcelDecommissionService.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedParcelComponentNames,
                parcelNamesFromImage)).thenReturn(new ParcelOperationStatus(Map.of("product3", "version3"), Map.of()));
        when(clouderaManagerParcelDecommissionService.undistributeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage)).thenReturn(new ParcelOperationStatus(Map.of("product3", "version3"), Map.of()));
        when(clouderaManagerParcelDecommissionService.removeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames,
                parcelNamesFromImage)).thenReturn(new ParcelOperationStatus(Map.of("product3", "version3"), Map.of()));

        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(usedComponents, parcelNamesFromImage);

        // THEN
        verify(clouderaManagerParcelDecommissionService).deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(),
                usedParcelComponentNames, parcelNamesFromImage);
        verify(clouderaManagerParcelDecommissionService).undistributeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage);
        verify(clouderaManagerParcelDecommissionService).removeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage);
        assertEquals(1, operationStatus.getSuccessful().size());
        assertEquals(0, operationStatus.getFailed().size());
    }

    @Test
    void removeUnusedParcelsWhenSomeParcelOperationsFail() {
        // GIVEN
        Set<String> parcelNamesFromImage = new HashSet<>();
        ClouderaManagerProduct cmProduct1 = createClouderaManagerProduct("product1", "version1");
        ClouderaManagerProduct cmProduct2 = createClouderaManagerProduct("product2", "version2");
        Set<ClusterComponentView> usedComponents = Set.of(createClusterComponent(cmProduct1), createClusterComponent(cmProduct2));
        Set<String> usedParcelComponentNames = Set.of(cmProduct1.getName(), cmProduct2.getName());
        when(clouderaManagerApiFactory.getParcelsResourceApi(v31Client)).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(v31Client)).thenReturn(parcelResourceApi);
        when(clouderaManagerParcelDecommissionService.deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, STACK_NAME, usedParcelComponentNames,
                parcelNamesFromImage))
                .thenReturn(new ParcelOperationStatus(Map.of("spark3", "version3", "product5", "version5"), Map.of("product4", "version4")));
        when(clouderaManagerParcelDecommissionService.undistributeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage))
                .thenReturn(new ParcelOperationStatus(Map.of("product5", "version5"), Map.of("spark3", "version3")));
        when(clouderaManagerParcelDecommissionService.removeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack, usedParcelComponentNames,
                parcelNamesFromImage)).thenReturn(new ParcelOperationStatus(Map.of("product5", "version5"), Map.of()));

        // WHEN
        ParcelOperationStatus operationStatus = underTest.removeUnusedParcels(usedComponents, parcelNamesFromImage);

        // THEN
        verify(clouderaManagerParcelDecommissionService, times(1)).deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(),
                usedParcelComponentNames, parcelNamesFromImage);
        verify(clouderaManagerParcelDecommissionService, times(1)).undistributeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage);
        verify(clouderaManagerParcelDecommissionService, times(1)).removeUnusedParcels(v31Client, parcelsResourceApi, parcelResourceApi, stack,
                usedParcelComponentNames, parcelNamesFromImage);
        assertEquals(1, operationStatus.getSuccessful().size());
        assertTrue(operationStatus.getSuccessful().containsEntry("product5", "version5"));
        assertEquals(2, operationStatus.getFailed().size());
        assertTrue(operationStatus.getFailed().containsEntry("spark3", "version3"));
        assertTrue(operationStatus.getFailed().containsEntry("product4", "version4"));
    }

    @Test
    void testIsServicePresent() throws ApiException {
        stack.getCluster().setName("test-cluster-name");
        List<ApiService> services = List.of(new ApiService().type("RANGER_RAZ"), new ApiService().type("ATLAS"), new ApiService().type("HDFS"));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(anyString(), anyString())).thenReturn(new ApiServiceList().items(services));

        assertTrue(underTest.isServicePresent(stack.getCluster().getName(), "RANGER_RAZ"));
        assertFalse(underTest.isServicePresent(stack.getCluster().getName(), "NON EXISTENT"));
    }

    @Test
    void testStopClusterWhenCmIsAlreadyStopped() throws CloudbreakException {

        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.checkCmStatus(stack, v31Client)).thenReturn(timeout);

        underTest.stopCluster(true);

        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmShutdown(stack, v31Client, apiCommand.getId());
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
        verify(eventService, times(0)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
    }

    @Test
    void testStopClusterWhenCmCallThrowsApiException() throws ApiException {

        when(clouderaManagerPollingServiceProvider.checkCmStatus(stack, v31Client)).thenReturn(success);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
        when(clustersResourceApi.stopCommand(stack.getName())).thenThrow(new ApiException("api exception"));
        List<ApiService> services = List.of(
                new ApiService().type("RANGER_RAZ").serviceState(ApiServiceState.STOPPED),
                new ApiService().type("ATLAS").serviceState(ApiServiceState.STARTED),
                new ApiService().type("HDFS").serviceState(ApiServiceState.STARTED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(anyString(), anyString())).thenReturn(new ApiServiceList().items(services));

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.stopCluster(true));
        assertEquals("api exception", exception.getMessage());
    }

    @Test
    void testHostsStartRoles() throws ApiException {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.9.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(1L);
        when(clouderaManagerResourceApi.hostsStartRolesCommand(any())).thenReturn(apiCommand);

        when(clouderaManagerPollingServiceProvider.startPollingStartRolesCommand(stack, v31Client, apiCommand.getId())).thenReturn(success);
        underTest.hostsStartRoles(List.of("fqdn1", "fqdn2"));
        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        verify(clouderaManagerResourceApi, times(1)).hostsStartRolesCommand(apiHostNameListArgumentCaptor.capture());
        ApiHostNameList apiHostNameList = apiHostNameListArgumentCaptor.getValue();
        assertThat(apiHostNameList.getItems()).containsOnly("fqdn1", "fqdn2");
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingStartRolesCommand(stack, v31Client, apiCommand.getId());
        ArgumentCaptor<ClusterCommand> clusterCommandArgumentCaptor = ArgumentCaptor.forClass(ClusterCommand.class);
        verify(clusterCommandService).save(clusterCommandArgumentCaptor.capture());
        ClusterCommand startRoleClusterCommand = clusterCommandArgumentCaptor.getValue();
        verify(clusterCommandService).delete(startRoleClusterCommand);
        assertEquals(ClusterCommandType.HOST_START_ROLES, startRoleClusterCommand.getClusterCommandType());
    }

    @Test
    void testHostsStartRolesIfCommandExists() throws ApiException {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.9.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(1L);
        ClusterCommand startRoleClusterCommand = new ClusterCommand();
        startRoleClusterCommand.setCommandId(apiCommand.getId());
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(CLUSTER_ID, ClusterCommandType.HOST_START_ROLES))
                .thenReturn(Optional.of(startRoleClusterCommand));

        when(clouderaManagerPollingServiceProvider.startPollingStartRolesCommand(stack, v31Client, apiCommand.getId())).thenReturn(success);
        underTest.hostsStartRoles(List.of("fqdn1", "fqdn2"));
        verify(clouderaManagerResourceApi, times(0)).hostsStartRolesCommand(any());
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingStartRolesCommand(stack, v31Client, apiCommand.getId());
        verify(clusterCommandService, times(0)).save(any());
        verify(clusterCommandService).delete(startRoleClusterCommand);
    }

    @Test
    void testHostsStartRolesButVersionIsLowerThan790() throws ApiException {
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("7.5.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        ApiCommand apiCommand = new ApiCommand();
        apiCommand.setId(1L);

        underTest.hostsStartRoles(List.of("fqdn1", "fqdn2"));
        verify(clouderaManagerResourceApi, times(0)).hostsStartRolesCommand(any());
        verify(clouderaManagerPollingServiceProvider, times(0)).startPollingStartRolesCommand(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStopClusterWhenCmIsNotStoppedAndNotStoppedServicesExistThenTheyAreStopped(boolean disableKnoxAutorestart) throws CloudbreakException, ApiException {

        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.checkCmStatus(stack, v31Client)).thenReturn(success);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
        when(clustersResourceApi.stopCommand(stack.getName())).thenReturn(apiCommand);
        when(clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, v31Client, apiCommand.getId())).thenReturn(success);
        List<ApiService> services = List.of(
                new ApiService().type("RANGER_RAZ").serviceState(ApiServiceState.STOPPED),
                new ApiService().type("ATLAS").serviceState(ApiServiceState.STARTED),
                new ApiService().type("HDFS").serviceState(ApiServiceState.STARTED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(anyString(), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.stopCluster(disableKnoxAutorestart);

        if (disableKnoxAutorestart) {
            verify(configService, times(1)).modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), false);
        } else {
            verify(configService, never()).modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), false);
        }
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmShutdown(stack, v31Client, apiCommand.getId());
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStopClusterWhenCmIsNotStoppedAndAllServicesStoppedThenTheyAreNotStopped(boolean disableKnoxAutorestart) throws CloudbreakException, ApiException {

        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.checkCmStatus(stack, v31Client)).thenReturn(success);
        List<ApiService> services = List.of(
                new ApiService().type("RANGER_RAZ").serviceState(ApiServiceState.STOPPED),
                new ApiService().type("ATLAS").serviceState(ApiServiceState.STOPPED),
                new ApiService().type("TEZ").serviceState(ApiServiceState.NA),
                new ApiService().type("HDFS").serviceState(ApiServiceState.STOPPING));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(anyString(), anyString())).thenReturn(new ApiServiceList().items(services));
        underTest.stopCluster(disableKnoxAutorestart);

        if (disableKnoxAutorestart) {
            verify(configService, times(1)).modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), false);
        } else {
            verify(configService, never()).modifyKnoxAutoRestartIfCmVersionAtLeast(CLOUDERAMANAGER_VERSION_7_1_0, v31Client, stack.getName(), false);
        }
        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmShutdown(stack, v31Client, apiCommand.getId());
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPED);
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_STOPPING);
    }

    @Test
    void testUpdateConfig() throws Exception {
        doNothing().when(clouderaManagerConfigModificationService).updateConfigs(any(), any(), any(), any());
        when(clouderaManagerConfigModificationService.getServiceNames(any(), any(), any())).thenReturn(List.of("test"));

        underTest.updateConfig(HashBasedTable.create(), CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG);

        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(any(), any(), eq(false), eq(false),
                eq(Optional.of(List.of("test"))));
    }

    @Test
    void testClusterRollingRestart() throws Exception {
        underTest.rollingRestartServices(false);
        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(any(), any(), eq(true), eq(false), any());

        underTest.rollingRestartServices(true);
        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(any(), any(), eq(true), eq(true), any());
    }

    @Test
    void testClusterRollingRestartWhenRollingUpgradeNotAvailable() throws Exception {
        doThrow(new ClouderaManagerOperationFailedException("Command Rolling Restart is not currently available for execution"))
                .when(clouderaManagerRestartService)
                .doRestartServicesIfNeeded(any(), any(), eq(true), eq(false), any());

        doNothing()
                .when(clouderaManagerRestartService)
                .doRestartServicesIfNeeded(any(), any(), eq(false), eq(false), any());

        underTest.rollingRestartServices(false);
        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(any(), any(), eq(true), eq(false), any());
        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(any(), any(), eq(false), eq(false), any());
    }

    @Test
    public void testReadServices() throws ApiException {
        List<ApiService> services = List.of(
                new ApiService().name("RANGER_RAZ").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("ATLAS").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("TEZ").serviceState(ApiServiceState.NA),
                new ApiService().name("HDFS").serviceState(ApiServiceState.STOPPING));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        Map<String, String> results = underTest.fetchServiceStatuses();

        assertEquals(4, results.size());
        assertEquals("STOPPED", results.get("ATLAS"));
        assertEquals("STOPPING", results.get("HDFS"));
        assertEquals("NA", results.get("TEZ"));
    }

    @Test
    public void testStartClusterWithOnlyServicesWhenNoActiveStartCommand() throws Exception {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER))).thenReturn(Optional.empty());
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(1L);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, 1L)).thenReturn(pollingResult);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        underTest.startCluster(true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, 1L);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while waiting for Cloudera Runtime services to start");
        verify(clusterCommandService, times(1)).delete(any(ClusterCommand.class));
    }

    @Test
    public void testStartClusterWithOnlyServicesWhenNoStartCommandInCM() throws Exception {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        ClusterCommand clusterCommand = new ClusterCommand();
        clusterCommand.setCommandId(1L);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(1L);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, 1L)).thenReturn(pollingResult);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        underTest.startCluster(true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, 1L);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while waiting for Cloudera Runtime services to start");
        verify(clusterCommandService, times(2)).delete(any(ClusterCommand.class));
    }

    @Test
    public void testStartClusterWithOnlyServicesWhenActiveStartCommand() throws Exception {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        ClusterCommand clusterCommand = new ClusterCommand();
        clusterCommand.setCommandId(1L);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.isActive()).thenReturn(Boolean.TRUE);
        when(clouderaManagerCommandsService.getApiCommandIfExist(v31Client, 1L)).thenReturn(Optional.of(startCommand));
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, 1L)).thenReturn(pollingResult);

        underTest.startCluster(true);

        verify(clustersResourceApi, never()).startCommand(STACK_NAME);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, 1L);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while waiting for Cloudera Runtime services to start");
        verify(clusterCommandService, times(1)).delete(any(ClusterCommand.class));
    }

    @Test
    public void testStartClusterWithOnlyServicesWhenInactiveStartCommand() throws Exception {
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        ClusterCommand clusterCommand = new ClusterCommand();
        clusterCommand.setCommandId(1L);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(clouderaManagerCommandsService.getApiCommandIfExist(v31Client, 1L)).thenReturn(Optional.of(startCommand));
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        ApiCommand newStartCommand = mock(ApiCommand.class);
        when(newStartCommand.getId()).thenReturn(1L);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(newStartCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, 1L)).thenReturn(pollingResult);

        underTest.startCluster(true);

        verify(clustersResourceApi, times(1)).startCommand(STACK_NAME);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, 1L);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while waiting for Cloudera Runtime services to start");
        verify(clusterCommandService, times(2)).delete(any(ClusterCommand.class));
    }

    @Test
    public void testGetStackCdhVersion() throws Exception {
        String expectedCdhVersion = "7.2.18";
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        ApiParcelList parcelList = new ApiParcelList();
        parcelList.addItemsItem(new ApiParcel().stage(ACTIVATED.name()).product(CDH).version(expectedCdhVersion));
        parcelList.addItemsItem(new ApiParcel().stage(ACTIVATED.name()).product(FLINK).version(expectedCdhVersion));
        when(parcelsResourceApi.readParcels(STACK_NAME, SUMMARY)).thenReturn(parcelList);
        String stackCdhVersion = underTest.getStackCdhVersion(STACK_NAME);
        assertEquals(expectedCdhVersion, stackCdhVersion);
    }

    @Test
    public void testGetStackCdhVersionNoActivatedParcel() throws Exception {
        String expectedCdhVersion = "7.2.18";
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        ApiParcelList parcelList = new ApiParcelList();
        parcelList.addItemsItem(new ApiParcel().stage("DISTRIBUTED").product(CDH).version(expectedCdhVersion));
        parcelList.addItemsItem(new ApiParcel().stage("DISTRIBUTED").product(FLINK).version(expectedCdhVersion));
        when(parcelsResourceApi.readParcels(STACK_NAME, SUMMARY)).thenReturn(parcelList);
        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.getStackCdhVersion(STACK_NAME));
    }

    @Test
    public void testEnableZookeeperMigrationMode() {
        underTest.enableZookeeperMigrationMode(stack);

        verify(clouderaManagerKraftMigrationService).enableZookeeperMigrationMode(eq(v31Client), eq(stack));
    }

    @Test
    public void testMigrateZookeeperToKraft() {
        underTest.migrateZookeeperToKraft(stack);

        verify(clouderaManagerKraftMigrationService).migrateZookeeperToKraft(eq(v31Client), eq(stack));
    }

    @Test
    public void testFinalizeZookeeperToKraftMigration() {
        underTest.finalizeZookeeperToKraftMigration(stack);

        verify(clouderaManagerKraftMigrationService).finalizeZookeeperToKraftMigration(eq(v31Client), eq(stack));
    }

    @Test
    public void testRollbackZookeeperToKraftMigration() {
        underTest.rollbackZookeeperToKraftMigration(stack);

        verify(clouderaManagerKraftMigrationService).rollbackZookeeperToKraftMigration(eq(v31Client), eq(stack));
    }

    @Test
    public void testStopClouderaManagerServiceWhenServiceIsStarted() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.STARTED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.stopClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(1)).stopClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    public void testStopClouderaManagerServiceWhenServiceIsAlreadyStopped() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.STOPPED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.stopClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(0)).stopClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    public void testStopClouderaManagerServiceWhenServiceStatusIsNA() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.NA));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.stopClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(0)).stopClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    public void testStartClouderaManagerServiceWhenServiceIsStopped() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.STOPPED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.startClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(1)).startClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    public void testStartClouderaManagerServiceWhenServiceIsAlreadyStarted() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.STARTED));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.startClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(0)).startClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    public void testStartClouderaManagerServiceWhenServiceStatusIsNA() throws ApiException {
        List<ApiService> services = List.of(new ApiService().name("yarn").serviceState(ApiServiceState.NA));
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(eq(STACK_NAME), anyString())).thenReturn(new ApiServiceList().items(services));

        underTest.startClouderaManagerService("YARN", true);

        verify(clouderaManagerServiceManagementService, times(0)).startClouderaManagerService(v31Client, stack, "YARN", true);
    }

    @Test
    void testReallocateMemoryDiffWhenVersionNotSupported() {
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_6_0.getVersion());
        ResetJvmParamsDiff result = underTest.reallocateMemoryDiff();
        assertTrue(result.getConfigsBefore().isEmpty());
        assertTrue(result.getConfigsAfter().isEmpty());
        verifyNoInteractions(hostsResourceApi);
    }

    @Test
    void testReallocateMemoryDiffSuccess() throws ApiException {
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_13_2_0.getVersion());
        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        setUpReadHosts(false);

        ApiConfigRecord configRecordBefore = new ApiConfigRecord()
                .name("heap_size").value("1024")
                .applicability(AutoConfigApplicability.RECONFIGURABLE);
        ApiConfigRecord configRecordAfter = new ApiConfigRecord()
                .name("heap_size").value("2048")
                .applicability(AutoConfigApplicability.RECONFIGURABLE);
        ApiHostReallocateMemoryResponse apiResponse = new ApiHostReallocateMemoryResponse()
                .configsBefore(List.of(configRecordBefore))
                .configsAfter(List.of(configRecordAfter));
        when(hostsResourceApi.reallocateMemoryDiff(any(ApiHostNameList.class))).thenReturn(apiResponse);

        ResetJvmParamsDiff result = underTest.reallocateMemoryDiff();
        ArgumentCaptor<ApiHostNameList> hostNameListCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        verify(hostsResourceApi).reallocateMemoryDiff(hostNameListCaptor.capture());
        assertEquals(List.of("original"), hostNameListCaptor.getValue().getItems());
        assertFalse(result.getConfigsBefore().isEmpty());
        assertEquals("heap_size", result.getConfigsBefore().getFirst().getName());
        assertEquals("1024", result.getConfigsBefore().getFirst().getValue());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE, result.getConfigsBefore().getFirst().getApplicability());
        assertFalse(result.getConfigsAfter().isEmpty());
        assertEquals("heap_size", result.getConfigsAfter().getFirst().getName());
        assertEquals("2048", result.getConfigsAfter().getFirst().getValue());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE, result.getConfigsAfter().getFirst().getApplicability());
        verify(eventService).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_CM_REALLOCATION_SUCCESSFUL);
    }


    @Test
    public void testUpdateTrustedRealmsCallsApiWithUpperCasedRealm() throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        TrustView trustView = new TrustView("10.0.0.1", "kdc.example.com", "example.com");

        underTest.updateTrustedRealms(trustView);

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        ApiBatchRequest batchRequest = batchRequestCaptor.getValue();
        assertThat(batchRequest.getItems()).hasSize(1);
        ApiBatchRequestElement element = batchRequest.getItems().get(0);
        assertEquals(HTTPMethod.PUT, element.getMethod());
        assertEquals("/clusters/" + STACK_NAME + "/services/core_settings/config", element.getUrl());
        assertThat(element.getBody()).isInstanceOf(ApiConfig.class);
        ApiConfig config = (ApiConfig) element.getBody();
        assertEquals("trusted_realms", config.getName());
        assertEquals("EXAMPLE.COM", config.getValue());
    }

    @Test
    public void testUpdateTrustedRealmsThrowsCloudManagerOperationFailedExceptionOnApiException() throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        when(batchResourceApi.execute(any(ApiBatchRequest.class))).thenThrow(new ApiException("CM API failure"));
        TrustView trustView = new TrustView("10.0.0.1", "kdc.example.com", "EXAMPLE.COM");

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.updateTrustedRealms(trustView));

        assertThat(exception.getMessage()).contains("CM API failure");
    }
}
