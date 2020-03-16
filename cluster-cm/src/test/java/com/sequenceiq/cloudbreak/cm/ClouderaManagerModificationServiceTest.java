package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCdhUpgradeArgs;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.model.ParcelResource;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerModificationServiceTest {

    private static final String STACK_NAME = "stack_name";

    private static final String HOST_GROUP_NAME = "host_group_name";

    private static final long CLUSTER_ID = 1L;

    private static final BigDecimal REFRESH_PARCEL_REPOS_ID = new BigDecimal(1);

    @InjectMocks
    private ClouderaManagerModificationService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient apiClientMock;

    @Spy
    private Stack stack = new Stack();

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private HostsResourceApi hostResourceApi;

    @Mock
    private HostTemplatesResourceApi hostTemplatesResourceApi;

    @Mock
    private MgmtServiceResourceApi mgmtServiceResourceApi;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private ParcelResourceApi parcelResourceApi;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    @Mock
    private ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    private Cluster cluster;

    private HostGroup hostGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        stack.setName(STACK_NAME);
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        hostGroup = new HostGroup();
        hostGroup.setName(HOST_GROUP_NAME);
    }

    @Test
    void upscaleClusterListHostsException() throws Exception {
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null))).thenThrow(new ApiException("Failed to get hosts"));
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        Exception exception = assertThrows(CloudbreakException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));
        assertEquals("Failed to upscale", exception.getMessage());
    }

    @Test
    void upscaleClusterNoHostToUpscale() throws Exception {
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        when(clouderaManagerRepo.getPredefined()).thenReturn(Boolean.TRUE);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);
        verify(clouderaManagerApiFactory, never()).getHostsResourceApi(any());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);
    }

    @Test
    void upscaleClusterRecovery() throws Exception {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(null);
        setUpListClusterHosts();
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);
        verify(clouderaManagerApiFactory, never()).getHostsResourceApi(any());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);
    }

    @Test
    void upscaleClusterTerminationOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts();
        setUpDeployClientConfigPolling(PollingResult.EXIT);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        Exception exception = assertThrows(CancellationException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));
        String exceptionMessage = "Cluster was terminated while waiting for client configurations to deploy";
        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());
    }

    private void setUpListClusterHosts() throws ApiException {
        ApiHostRefList clusterHostsRef = new ApiHostRefList().items(List.of(new ApiHostRef().hostname("original")));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null))).thenReturn(clusterHostsRef);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);
    }

    @Test
    void upscaleClusterTimeoutOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts();
        setUpDeployClientConfigPolling(PollingResult.TIMEOUT);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        Exception exception = assertThrows(CloudbreakException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));
        String exceptionMessage = "Timeout while Cloudera Manager deployed client configurations.";
        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());
    }

    @Test
    void upscaleCluster() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts();
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), any(ApiHostRefList.class)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(apiClientMock))).thenReturn(hostTemplatesResourceApi);

        PollingResult applyTemplatePollingResult = PollingResult.SUCCESS;
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(apiClientMock), eq(applyHostTemplateCommandId)))
                .thenReturn(applyTemplatePollingResult);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), applyTemplateBodyCatcher.capture());

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());
    }

    @Test
    void testUpgradeClusterComponentIsNotPresent() {
        Set<ClusterComponent> clusterComponents = TestUtil.clusterComponentSet(cluster);
        Set<ClusterComponent> clusterComponentsNoCDH
                = clusterComponents.stream().filter(clusterComponent -> !clusterComponent.getName().equals("CDH")).collect(Collectors.toSet());

        cluster.setComponents(clusterComponentsNoCDH);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.upgradeClusterRuntime(clusterComponentsNoCDH));
        Assertions.assertEquals("Runtime component not found!", exception.getMessage());
    }

    @Test
    void testUpgradeCluster() throws CloudbreakException, ApiException {
        TestUtil.clusterComponents(cluster);

        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);

        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;
        ParcelResource parcelResource = new ParcelResource(stack.getName(), TestUtil.CDH, TestUtil.CDH_VERSION);

        // Start download
        when(parcelResourceApi.startDownloadCommand(eq(stack.getName()), eq(TestUtil.CDH), eq(TestUtil.CDH_VERSION)))
                .thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDownload(stack, apiClientMock, apiCommandId, parcelResource))
                .thenReturn(successPollingResult);

        // Start distribute
        when(parcelResourceApi.startDistributionCommand(eq(stack.getName()), eq(TestUtil.CDH), eq(TestUtil.CDH_VERSION)))
                .thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeParcelDistribute(stack, apiClientMock, apiCommandId, parcelResource))
                .thenReturn(successPollingResult);

        // Upgrade
        ApiCdhUpgradeArgs upgradeArgs = new ApiCdhUpgradeArgs();
        upgradeArgs.setCdhParcelVersion(TestUtil.CDH_VERSION);
        when(clustersResourceApi.upgradeCdhCommand(eq(stack.getName()), eq(upgradeArgs))).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);

        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.STALE);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        when(servicesResourceApi.readServices(stack.getName(), "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);
        when(clustersResourceApi.deployClientConfigsAndRefresh(stack.getName())).thenReturn(new ApiCommand().id(apiCommandId));
        when(clustersResourceApi.refresh(stack.getName())).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);

        underTest.upgradeClusterRuntime(cluster.getComponents());

        verify(clouderaManagerResourceApi, times(1)).updateConfig(any(), any());
        verify(parcelResourceApi, times(1)).startDownloadCommand(stack.getName(), TestUtil.CDH, TestUtil.CDH_VERSION);
        verify(parcelResourceApi, times(1)).startDistributionCommand(stack.getName(), TestUtil.CDH, TestUtil.CDH_VERSION);
        verify(clustersResourceApi, times(1)).upgradeCdhCommand(stack.getName(), upgradeArgs);
        verify(clustersResourceApi, times(1)).deployClientConfigsAndRefresh(stack.getName());
        verify(clustersResourceApi, times(1)).refresh(stack.getName());

    }

    @Test
    void testPollRefreshWhenCancelled() {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(PollingResult.EXIT);
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals("Cluster was terminated while waiting for service refresh", actual.getMessage());
    }

    @Test
    void testPollRefreshWhenTimeout() {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(PollingResult.TIMEOUT);
        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals("Timeout while Cloudera Manager was refreshing services.", actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenCancelled() {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(PollingResult.EXIT);
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollDeployConfig(apiCommand));
        assertEquals("Cluster was terminated while waiting for config deploy", actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenTimeout() {
        ApiCommand apiCommand = new ApiCommand();
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(PollingResult.TIMEOUT);
        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.pollDeployConfig(apiCommand));
        assertEquals("Timeout while Cloudera Manager was config deploying services.", actual.getMessage());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenNoStaleConfig() throws CloudbreakException, ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.FRESH)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi);

        verify(clouderaManagerPollingServiceProvider, times(0)).startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenConfigStale() throws CloudbreakException, ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        List<ApiCommand> apiCommands = List.of(
                new ApiCommand().name("DeployClusterClientConfig").id(BigDecimal.ONE),
                new ApiCommand().name("RefreshCluster").id(BigDecimal.ONE)
        );
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), any());
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmConfigurationRefresh(eq(stack), eq(apiClientMock), any());
    }

    private void setUpReadHosts() throws ApiException {
        ApiHostList apiHostsRef = new ApiHostList().items(
                List.of(new ApiHost().hostname("original"), new ApiHost().hostname("upscaled")));
        when(hostResourceApi.readHosts(eq(null), eq(null), eq("SUMMARY"))).thenReturn(apiHostsRef);
        when(clouderaManagerApiFactory.getHostsResourceApi(eq(apiClientMock))).thenReturn(hostResourceApi);
    }

    private void setUpDeployClientConfigPolling(PollingResult success) throws ApiException {
        BigDecimal deployClientCommandId = new BigDecimal(100);
        when(clustersResourceApi.deployClientConfig(eq(STACK_NAME))).thenReturn(new ApiCommand().id(deployClientCommandId));

        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), eq(deployClientCommandId)))
                .thenReturn(success);
    }
}