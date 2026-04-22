package com.sequenceiq.cloudbreak.cm;

import static com.cloudera.api.swagger.model.ApiServiceState.STARTED;
import static com.cloudera.api.swagger.model.ApiServiceState.STOPPED;
import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerServiceManagementServiceTest {

    private static final String TEST_CLUSTER_NAME = "test-cluster-name";

    private static final String SERVICE_TYPE = "YARN";

    private static final String SERVICE_NAME = "yarn-1";

    private static final Long COMMAND_ID = Long.valueOf(123L);

    @InjectMocks
    private ClouderaManagerServiceManagementService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ClusterCommandService clusterCommandService;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Mock
    private ApiClient apiClient;

    @Mock
    private StackDtoDelegate stack;

    @BeforeEach
    void before() {
        lenient().when(stack.getName()).thenReturn(TEST_CLUSTER_NAME);
    }

    @Test
    void testReadServices() throws ApiException {
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(new ApiServiceList());

        underTest.readServices(apiClient, "cluster");

        verify(servicesResourceApi).readServices(any(), any());
    }

    @Test
    public void testReadServicesFailure() throws ApiException {
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices(any(), any())).thenThrow(new ApiException("something"));

        assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.readServices(apiClient, "cluster"));

        verify(servicesResourceApi).readServices(any(), any());
    }

    @Test
    public void testStopServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingServiceStop(stack, apiClient, COMMAND_ID)).thenReturn(mock(ExtendedPollingResult.class));

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(1)).stopCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceStop(stack, apiClient, COMMAND_ID);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testStopServiceSuccessWhenNotNecessaryToWaitForCommandExecution() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, false);

        verify(servicesResourceApi, times(1)).stopCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verifyNoInteractions(clouderaManagerPollingServiceProvider);
        verifyNoInteractions(pollingResultErrorHandler);
    }

    @Test
    public void testStopServiceWhenTheServiceAlreadyStopped() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE).serviceState(STOPPED));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStopServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.stopClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).stopCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStartServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(servicesResourceApi.startCommand(TEST_CLUSTER_NAME, SERVICE_NAME)).thenReturn(new ApiCommand().id(COMMAND_ID));
        when(clouderaManagerPollingServiceProvider.startPollingServiceStart(stack, apiClient, COMMAND_ID)).thenReturn(mock(ExtendedPollingResult.class));

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(1)).startCommand(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceStart(stack, apiClient, COMMAND_ID);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testStopServiceWhenTheServiceAlreadyStarted() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE).serviceState(STARTED));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).startCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testStartServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.startClouderaManagerService(apiClient, stack, SERVICE_TYPE, true);

        verify(servicesResourceApi, times(0)).startCommand(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    public void testDeleteServiceSuccess() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type(SERVICE_TYPE));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(clouderaManagerPollingServiceProvider.startPollingServiceDeletion(stack, apiClient, SERVICE_TYPE))
                .thenReturn(mock(ExtendedPollingResult.class));

        underTest.deleteClouderaManagerService(apiClient, stack, SERVICE_TYPE);

        verify(servicesResourceApi, times(1)).deleteService(eq(TEST_CLUSTER_NAME), eq(SERVICE_NAME));
        verify(clouderaManagerPollingServiceProvider).startPollingServiceDeletion(stack, apiClient, SERVICE_TYPE);
        verify(pollingResultErrorHandler).handlePollingResult(any(ExtendedPollingResult.class), any(), any());
    }

    @Test
    public void testDeleteServiceNoServiceFound() throws Exception {
        ApiServiceList apiServiceList = new ApiServiceList().addItemsItem(new ApiService().name(SERVICE_NAME).type("HUE"));
        when(servicesResourceApi.readServices(TEST_CLUSTER_NAME, DataView.SUMMARY.name())).thenReturn(apiServiceList);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);

        underTest.deleteClouderaManagerService(apiClient, stack, SERVICE_TYPE);

        verify(servicesResourceApi, times(0)).deleteService(TEST_CLUSTER_NAME, SERVICE_NAME);
    }

    @Test
    void stopAllClusterRuntimeServicesPollsActiveCommandBeforeClusterStop() throws Exception {
        ApiService hdfsStarted = new ApiService().type("HDFS").serviceState(ApiServiceState.STARTED);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        ApiCommand blocking = new ApiCommand().id(99L).name("RefreshCluster");
        when(clustersResourceApi.listActiveCommands(eq(TEST_CLUSTER_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of(blocking)))
                .thenReturn(new ApiCommandList().items(List.of()));

        ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(99L), anyString())).thenReturn(success);

        ApiCommand stopCmd = new ApiCommand().id(200L);
        when(clustersResourceApi.stopCommand(TEST_CLUSTER_NAME)).thenReturn(stopCmd);
        when(clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, apiClient, 200L)).thenReturn(success);

        underTest.stopAllClusterRuntimeServices(apiClient, stack, TEST_CLUSTER_NAME, List.of(hdfsStarted));

        InOrder inOrder = inOrder(clouderaManagerPollingServiceProvider, clustersResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 99L, "Active CM cluster command: RefreshCluster");
        inOrder.verify(clustersResourceApi).stopCommand(TEST_CLUSTER_NAME);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmShutdown(stack, apiClient, 200L);
        verify(clustersResourceApi, times(2)).listActiveCommands(TEST_CLUSTER_NAME, SUMMARY, null);
        verify(pollingResultErrorHandler, times(2)).handlePollingResult(eq(success), any(), any());
    }

    @Test
    void startAllClusterRuntimeServicesPollsActiveCommandBeforeClusterStart() throws Exception {
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(42L);

        ApiService hdfsStopped = new ApiService().type("HDFS").serviceState(ApiServiceState.STOPPED);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        ApiCommand blocking = new ApiCommand().id(88L).name("DeployClusterClientConfig");
        when(clustersResourceApi.listActiveCommands(eq(TEST_CLUSTER_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of(blocking)))
                .thenReturn(new ApiCommandList().items(List.of()));

        ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(88L), anyString())).thenReturn(success);

        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(42L, ClusterCommandType.START_CLUSTER)).thenReturn(Optional.empty());
        ApiCommand cmStart = new ApiCommand().id(300L);
        when(clustersResourceApi.startCommand(TEST_CLUSTER_NAME)).thenReturn(cmStart);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient, 300L)).thenReturn(success);

        underTest.startAllClusterRuntimeServices(apiClient, stack, clusterView, List.of(hdfsStopped));

        InOrder inOrder = inOrder(clouderaManagerPollingServiceProvider, clustersResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 88L,
                "Active CM cluster command: DeployClusterClientConfig");
        inOrder.verify(clustersResourceApi).startCommand(TEST_CLUSTER_NAME);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClient, 300L);
        verify(clustersResourceApi, times(2)).listActiveCommands(TEST_CLUSTER_NAME, SUMMARY, null);
        verify(clusterCommandService).delete(any(ClusterCommand.class));
        verify(pollingResultErrorHandler, times(2)).handlePollingResult(eq(success), any(), any());
        verifyNoInteractions(clouderaManagerCommandsService);
    }

    @Test
    void stopAllClusterRuntimeServicesPollsAgainWhenNewActiveCommandAppearsAfterFirstCompletes() throws Exception {
        ApiService hdfsStarted = new ApiService().type("HDFS").serviceState(ApiServiceState.STARTED);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        ApiCommand firstBlocking = new ApiCommand().id(99L).name("RefreshCluster");
        ApiCommand secondBlocking = new ApiCommand().id(100L).name("ParcelsActivate");
        when(clustersResourceApi.listActiveCommands(eq(TEST_CLUSTER_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of(firstBlocking)))
                .thenReturn(new ApiCommandList().items(List.of(secondBlocking)))
                .thenReturn(new ApiCommandList().items(List.of()));

        ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(99L), anyString())).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(100L), anyString())).thenReturn(success);

        ApiCommand stopCmd = new ApiCommand().id(200L);
        when(clustersResourceApi.stopCommand(TEST_CLUSTER_NAME)).thenReturn(stopCmd);
        when(clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, apiClient, 200L)).thenReturn(success);

        underTest.stopAllClusterRuntimeServices(apiClient, stack, TEST_CLUSTER_NAME, List.of(hdfsStarted));

        InOrder inOrder = inOrder(clouderaManagerPollingServiceProvider, clustersResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 99L, "Active CM cluster command: RefreshCluster");
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 100L, "Active CM cluster command: ParcelsActivate");
        inOrder.verify(clustersResourceApi).stopCommand(TEST_CLUSTER_NAME);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmShutdown(stack, apiClient, 200L);
        verify(clustersResourceApi, times(3)).listActiveCommands(TEST_CLUSTER_NAME, SUMMARY, null);
        verify(pollingResultErrorHandler, times(3)).handlePollingResult(eq(success), any(), any());
    }

    @Test
    void startAllClusterRuntimeServicesPollsAgainWhenNewActiveCommandAppearsAfterFirstCompletes() throws Exception {
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(42L);

        ApiService hdfsStopped = new ApiService().type("HDFS").serviceState(ApiServiceState.STOPPED);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        ApiCommand firstBlocking = new ApiCommand().id(77L).name("DeployClusterClientConfig");
        ApiCommand secondBlocking = new ApiCommand().id(78L).name("RestartRoles");
        when(clustersResourceApi.listActiveCommands(eq(TEST_CLUSTER_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of(firstBlocking)))
                .thenReturn(new ApiCommandList().items(List.of(secondBlocking)))
                .thenReturn(new ApiCommandList().items(List.of()));

        ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(77L), anyString())).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(78L), anyString())).thenReturn(success);

        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(42L, ClusterCommandType.START_CLUSTER)).thenReturn(Optional.empty());
        ApiCommand cmStart = new ApiCommand().id(300L);
        when(clustersResourceApi.startCommand(TEST_CLUSTER_NAME)).thenReturn(cmStart);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, apiClient, 300L)).thenReturn(success);

        underTest.startAllClusterRuntimeServices(apiClient, stack, clusterView, List.of(hdfsStopped));

        InOrder inOrder = inOrder(clouderaManagerPollingServiceProvider, clustersResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 77L,
                "Active CM cluster command: DeployClusterClientConfig");
        inOrder.verify(clouderaManagerPollingServiceProvider).startDefaultPolling(stack, apiClient, 78L,
                "Active CM cluster command: RestartRoles");
        inOrder.verify(clustersResourceApi).startCommand(TEST_CLUSTER_NAME);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClient, 300L);
        verify(clustersResourceApi, times(3)).listActiveCommands(TEST_CLUSTER_NAME, SUMMARY, null);
        verify(clusterCommandService).delete(any(ClusterCommand.class));
        verify(pollingResultErrorHandler, times(3)).handlePollingResult(eq(success), any(), any());
        verifyNoInteractions(clouderaManagerCommandsService);
    }

    @Test
    void stopAllClusterRuntimeServicesThrowsWhenActiveClusterCommandsNeverDrainWithinMaxRounds() throws Exception {
        ApiService hdfsStarted = new ApiService().type("HDFS").serviceState(ApiServiceState.STARTED);
        when(clouderaManagerApiFactory.getClustersResourceApi(apiClient)).thenReturn(clustersResourceApi);

        ApiCommand perpetual = new ApiCommand().id(1L).name("StuckCommand");
        when(clustersResourceApi.listActiveCommands(eq(TEST_CLUSTER_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of(perpetual)));

        ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        when(clouderaManagerPollingServiceProvider.startDefaultPolling(eq(stack), eq(apiClient), eq(1L), anyString())).thenReturn(success);

        ClouderaManagerOperationFailedException ex = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.stopAllClusterRuntimeServices(apiClient, stack, TEST_CLUSTER_NAME, List.of(hdfsStarted)));

        assertTrue(ex.getMessage().contains("Stopped waiting"));
        verify(clouderaManagerPollingServiceProvider, times(10)).startDefaultPolling(stack, apiClient, 1L,
                "Active CM cluster command: StuckCommand");
        verify(clustersResourceApi, never()).stopCommand(eq(TEST_CLUSTER_NAME));
        verify(pollingResultErrorHandler, times(10)).handlePollingResult(eq(success), any(), any());
    }

}