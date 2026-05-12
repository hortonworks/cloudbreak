package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_13_1_500;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_5_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiEntityTag;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class ClouderaManagerModificationServiceUpgradeTest extends ClouderaManagerModificationServiceTestBase {

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndNoPostUpgradeCommandIsAvailable() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        Long apiCommandId = 200L;
        when(stack.getJavaVersion()).thenReturn(17);

        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_3.getVersion());

        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client)).thenReturn(success);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        Set<ClouderaManagerProduct> products = TestUtil.clouderaManagerProducts();

        underTest.upgradeClusterRuntime(products, true, Optional.empty(), false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(products, clouderaManagerResourceApi);
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerRestartService).doRestartServicesIfNeeded(v31Client, stack, false, false, Optional.empty());
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verifyNoInteractions(clouderaManagerUpgradeService);

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clustersResourceApi,
                clouderaManagerRestartService);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, v31Client);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(products, clouderaManagerResourceApi);
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerRestartService).doRestartServicesIfNeeded(v31Client, stack, false, false, Optional.empty());
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndPostUpgradeCommandIsAvailable()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        Long apiCommandId = 200L;

        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(stack.getJavaVersion()).thenReturn(17);

        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);

        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());

        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client)).thenReturn(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(HOSTNAME);
        instanceGroup.setGroupName(GROUP_NAME);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        ApiHostList clusterHostsRef = new ApiHostList().items(List.of(new ApiHost().hostname(HOSTNAME)));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenReturn(clusterHostsRef);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_6_0.getVersion());
        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostResourceApi);
        when(hostResourceApi.addTags(eq(HOSTNAME), any())).thenReturn(List.of());
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER))).thenReturn(Optional.empty());
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));
        when(clouderaManagerApiClientProvider.getV45Client(any(), any(), any(), any())).thenReturn(v31Client);
        Set<ClouderaManagerProduct> products = TestUtil.clouderaManagerProducts();

        underTest.upgradeClusterRuntime(products, true, Optional.empty(), false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(products, clouderaManagerResourceApi);
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmServicesRestart(stack, v31Client, apiCommandId);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clustersResourceApi, times(1)).startCommand(STACK_NAME);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, v31Client);
        verify(clouderaManagerRestartService, times(2)).waitForRestartExecutionIfPresent(v31Client, stack, false);
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());
        ArgumentCaptor<List<ApiEntityTag>> entityTagListCaptor = ArgumentCaptor.forClass(List.class);
        verify(hostResourceApi, times(1)).addTags(eq(HOSTNAME), entityTagListCaptor.capture());
        assertEquals("_cldr_cm_host_template_name", entityTagListCaptor.getValue().get(0).getName());
        assertEquals(GROUP_NAME, entityTagListCaptor.getValue().get(0).getValue());
        verify(hostsResourceApi, times(0)).reallocateMemory(any(), anyBoolean());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService,
                clustersResourceApi, clouderaManagerUpgradeService, clouderaManagerApiClientProvider, clouderaManagerRestartService);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, v31Client);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(products, clouderaManagerResourceApi);
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clustersResourceApi).startCommand(STACK_NAME);
        inOrder.verify(clouderaManagerApiClientProvider).getV45Client(any(), any(), any(), any());
        inOrder.verify(clouderaManagerUpgradeService).callPostRuntimeUpgradeCommand(eq(clustersResourceApi), eq(stack), eq(v31Client));
        inOrder.verify(clouderaManagerRestartService).doRestartServicesIfNeeded(v31Client, stack, false, false,
                Optional.empty());
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndPostUpgradeCommandIsAvailableAndRestartIsRunning()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        ApiHostList clusterHostsRef = new ApiHostList().items(List.of(new ApiHost().hostname(HOSTNAME)));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenReturn(clusterHostsRef);
        setUpReadHosts(false);
        Long apiCommandId = 200L;
        when(stack.getJavaVersion()).thenReturn(17);

        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);

        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_13_1_500.getVersion());

        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client)).thenReturn(success);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_13_1_500.getVersion());
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));
        when(clouderaManagerApiClientProvider.getV45Client(any(), any(), any(), any())).thenReturn(v31Client);
        Set<ClouderaManagerProduct> products = TestUtil.clouderaManagerProducts();

        underTest.upgradeClusterRuntime(products, true, Optional.empty(), false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(products, clouderaManagerResourceApi);
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerRestartService, times(2)).waitForRestartExecutionIfPresent(v31Client, stack, false);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clustersResourceApi, times(1)).startCommand(STACK_NAME);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, v31Client);
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());

        ArgumentCaptor<ApiHostNameList> apiHostNameListArgumentCaptor = ArgumentCaptor.forClass(ApiHostNameList.class);
        verify(hostsResourceApi, times(1)).reallocateMemory(apiHostNameListArgumentCaptor.capture(), eq(false));
        assertEquals("original", apiHostNameListArgumentCaptor.getValue().getItems().getFirst());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clustersResourceApi,
                clouderaManagerApiClientProvider, clouderaManagerUpgradeService);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, v31Client);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(products, clouderaManagerResourceApi);
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clustersResourceApi).startCommand(STACK_NAME);
        inOrder.verify(clouderaManagerApiClientProvider).getV45Client(any(), any(), any(), any());
        inOrder.verify(clouderaManagerUpgradeService).callPostRuntimeUpgradeCommand(eq(clustersResourceApi), eq(stack), eq(v31Client));
    }

    @Test
    void testUpgradeClusterWhenNotPatchUpgrade() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerApiFactory.getHostsResourceApi(any())).thenReturn(hostsResourceApi);
        ApiHostList clusterHostsRef = new ApiHostList().items(List.of(new ApiHost().hostname(HOSTNAME)));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenReturn(clusterHostsRef);
        setUpReadHosts(false);

        Long apiCommandId = 200L;
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(stack.getJavaVersion()).thenReturn(17);

        ApiService apiService = new ApiService()
                .name("SERVICE")
                .configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.STALE);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenReturn(new ApiCommand().id(apiCommandId));
        when(servicesResourceApi.readServices(stack.getName(), "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        when(clustersResourceApi.startCommand(stack.getName())).thenReturn(new ApiCommand().id(apiCommandId));
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, v31Client, apiCommandId))
                .thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_13_1_500.getVersion());
        Set<ClouderaManagerProduct> products = TestUtil.clouderaManagerProducts();
        Set<ClouderaManagerProduct> nonCdhProduct = Set.of(TestUtil.nonCdhProduct());
        when(clouderaManagerProductsProvider.getNonCdhProducts(products)).thenReturn(nonCdhProduct);

        underTest.upgradeClusterRuntime(products, false, Optional.empty(), true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(products, clouderaManagerResourceApi);
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(nonCdhProduct, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        verify(clouderaManagerCommonCommandService, times(1)).getApiCommand(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clouderaManagerUpgradeService,
                clustersResourceApi, clouderaManagerCommonCommandService, servicesResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, v31Client);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(products, clouderaManagerResourceApi);
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(products, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(nonCdhProduct, parcelResourceApi, parcelsResourceApi, stack, v31Client);
        inOrder.verify(servicesResourceApi).readServices(stack.getName(), "SUMMARY");
        inOrder.verify(clouderaManagerCommonCommandService).getApiCommand(any(), any(), any(), any());
    }
}
