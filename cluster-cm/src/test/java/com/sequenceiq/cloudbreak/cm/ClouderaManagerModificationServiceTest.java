package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_1_0;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_5_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_6_0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.BatchResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.MgmtServiceResourceApi;
import com.cloudera.api.swagger.ParcelResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiBatchResponseElement;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiConfigStalenessStatus;
import com.cloudera.api.swagger.model.ApiEntityTag;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostNameList;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.google.common.collect.HashBasedTable;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.config.ClouderaManagerFlinkConfigurationService;
import com.sequenceiq.cloudbreak.cm.config.modification.ClouderaManagerConfigModificationService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

import okhttp3.Call;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerModificationServiceTest {

    private static final String STACK_NAME = "stack_name";

    private static final String HOST_GROUP_NAME = "host_group_name";

    private static final long CLUSTER_ID = 1L;

    private static final BigDecimal REFRESH_PARCEL_REPOS_ID = new BigDecimal(1);

    private static final String HOSTNAME = "host1";

    private static final String GROUP_NAME = "group1";

    @InjectMocks
    private ClouderaManagerModificationService underTest;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient v31Client;

    @Mock
    private ApiClient v52Client;

    @Spy
    private Stack stack;

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
    private ParcelsResourceApi parcelsResourceApi;

    @Mock
    private BatchResourceApi batchResourceApi;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private ClouderaManagerRepo clouderaManagerRepo;

    @Mock
    private ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private ClouderaManagerConfigService configService;

    @Mock
    private ClouderaManagerParcelDecommissionService clouderaManagerParcelDecommissionService;

    @Mock
    private ClouderaManagerParcelManagementService clouderaManagerParcelManagementService;

    @Mock
    private ClouderaManagerUpgradeService clouderaManagerUpgradeService;

    @Mock
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    private ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerRestartService clouderaManagerRestartService;

    @Mock
    private ClouderaManagerConfigModificationService clouderaManagerConfigModificationService;

    @Mock
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    @Mock
    private ClusterCommandService clusterCommandService;

    @Mock
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Spy
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private ClouderaManagerFlinkConfigurationService clouderaManagerFlinkConfigurationService;

    private Cluster cluster;

    private HostGroup hostGroup;

    private final ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();

    private final ExtendedPollingResult exit = new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build();

    private final ExtendedPollingResult timeout = new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        stack.setName(STACK_NAME);
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setName(STACK_NAME);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        hostGroup = new HostGroup();
        hostGroup.setName(HOST_GROUP_NAME);
    }

    @Test
    void upscaleClusterListHostsException() throws Exception {
        ApiException apiException = new ApiException("Failed to get hosts");
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenThrow(apiException);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        CloudbreakException exception = assertThrows(CloudbreakException.class, () -> underTest.upscaleCluster(Map.of(hostGroup,
                new LinkedHashSet<>(instanceMetaDataList))));

        assertEquals("Failed to upscale", exception.getMessage());
        assertThat(exception).hasCauseReference(apiException);
    }

    @Test
    void upscaleClusterNoHostToUpscale() throws Exception {
        setUpDeployClientConfigPolling(success);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, v31Client, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(false);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();
        setUpReadHosts(false);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        verify(clouderaManagerResourceApi).refreshParcelRepos();
        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterNoHostToUpscaleAndPrewarmedImage() throws Exception {
        setUpDeployClientConfigPolling(success);

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(true);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();
        setUpReadHosts(false);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        verify(clouderaManagerResourceApi, never()).refreshParcelRepos();
        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTestWhenNoHostToUpscaleButRackIdUpdatedForOutdatedClusterHost() throws Exception {
        setUpDeployClientConfigPolling(success);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, v31Client, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(false);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();
        setUpReadHosts(false);

        setUpBatchSuccess();

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        instanceMetaData.setRackId("/originalRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "originalId", "/originalRack");
    }

    @Test
    void upscaleClusterTestWhenNoHostToUpscaleAndRackIdNotUpdatedForClusterHost() throws Exception {
        setUpDeployClientConfigPolling(success);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, v31Client, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(false);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();
        setUpReadHosts(false, "/originalRack");

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        instanceMetaData.setRackId("/originalRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterRecovery() throws Exception {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, v31Client, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, v31Client, REFRESH_PARCEL_REPOS_ID, Collections.emptyList()))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setPredefined(false);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        setUpListClusterHosts();
        setUpReadHosts(false);
        setUpDeployClientConfigPolling(success);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTerminationOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(exit);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        String exceptionMessage = "Cluster was terminated while waiting for config deploy";
        doThrow(new CancellationException(exceptionMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(exit), any(), any());

        CancellationException exception = assertThrows(CancellationException.class,
                () -> underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList))));

        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        verify(clouderaManagerRoleRefreshService, never()).refreshClusterRoles(any(ApiClient.class), any(Stack.class));
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    private void setUpListClusterHosts() throws ApiException {
        ApiHostList clusterHostsRef = new ApiHostList().items(List.of(new ApiHost().hostname("original")));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenReturn(clusterHostsRef);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
    }

    @Test
    void upscaleClusterTimeoutOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(timeout);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        String exceptionMessage = "Timeout while Cloudera Manager was config deploying services.";

        doThrow(new CloudbreakException(exceptionMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(timeout), any(), any());

        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList))));

        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        verify(clouderaManagerRoleRefreshService, never()).refreshClusterRoles(any(ApiClient.class), any(Stack.class));
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterWhenCmDoesNotSupportV52Api() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(success);
        setUpBatchSuccess();
        ReflectionTestUtils.setField(underTest, "v52Client", null);

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.FALSE),
                eq(Boolean.TRUE))).thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v31Client))).thenReturn(hostTemplatesResourceApi);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.9.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        instanceMetaData.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).isEqualTo(List.of("upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), applyTemplateBodyCatcher.capture(), eq(Boolean.FALSE), eq(Boolean.TRUE));

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterWhenCmDoesSupportV52Api() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(success);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE),
                eq(Boolean.TRUE))).thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);

        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        instanceMetaData.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).isEqualTo(List.of("upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), applyTemplateBodyCatcher.capture(), eq(Boolean.TRUE), eq(Boolean.TRUE));

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterSkipApplyingHostTemplatesIfHostListIsEmpy() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(false);
        setUpDeployClientConfigPolling(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        instanceMetaData.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).isEqualTo(List.of("upscaled"));

        verify(hostTemplatesResourceApi, never())
                .applyHostTemplate(anyString(), anyString(), any(), anyBoolean(), anyBoolean());
    }

    private void setUpBatchWithResponseAnswer(Answer<ApiBatchResponse> batchResponseAnswer) throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        when(batchResourceApi.execute(any(ApiBatchRequest.class))).thenAnswer(batchResponseAnswer);
    }

    private void setUpBatchSuccess() throws ApiException {
        setUpBatchWithResponseAnswer(invocation -> {
            ApiBatchRequest batchRequest = invocation.getArgument(0, ApiBatchRequest.class);
            int responseItemCount = batchRequest.getItems().size();
            ApiBatchResponse batchResponse = new ApiBatchResponse().success(true).items(new ArrayList<>());
            for (int i = 0; i < responseItemCount; i++) {
                batchResponse.addItemsItem(new ApiBatchResponseElement());
            }
            return batchResponse;
        });
    }

    private void verifyRackIdBatch(ApiBatchRequest batchRequest, String hostIdExpected, String rackIdExpected) {
        assertThat(batchRequest).isNotNull();

        List<ApiBatchRequestElement> batchRequestElements = batchRequest.getItems();
        assertThat(batchRequestElements).isNotNull();
        assertThat(batchRequestElements).hasSize(1);

        ApiBatchRequestElement batchRequestElement = batchRequestElements.get(0);
        assertThat(batchRequestElement.getMethod()).isEqualTo(HTTPMethod.PUT);
        assertThat(batchRequestElement.getUrl()).isEqualTo("/api/v31/hosts/" + hostIdExpected);
        assertThat(batchRequestElement.getAcceptType()).isEqualTo("application/json");
        assertThat(batchRequestElement.getContentType()).isEqualTo("application/json");
        assertThat(batchRequestElement.getBody()).isInstanceOf(ApiHost.class);
        ApiHost host = (ApiHost) batchRequestElement.getBody();
        assertThat(host.getRackId()).isEqualTo(rackIdExpected);
    }

    static Object[][] upscaleClusterTestWhenRackIdBatchExecutionFailureDataProvider() {
        return new Object[][]{
                // testCaseName batchResponseFactory
                {"response=null", (Supplier<ApiBatchResponse>) () -> null},
                {"success=null", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(null).items(List.of())},
                {"items=null", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(true).items(null)},
                {"success=false", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(false).items(List.of())},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("upscaleClusterTestWhenRackIdBatchExecutionFailureDataProvider")
    void upscaleClusterTestWhenRackIdBatchExecutionFailure(String testCaseName, Supplier<ApiBatchResponse> batchResponseFactory) throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);

        setUpBatchWithResponseAnswer(invocation -> batchResponseFactory.get());

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList))));

        assertThat(exception).hasMessageStartingWith("Setting rack ID for hosts batch operation failed. Response: ");

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService, never()).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        verify(hostTemplatesResourceApi, never()).applyHostTemplate(anyString(), anyString(), any(ApiHostRefList.class), anyBoolean(), anyBoolean());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterTestWhenRackIdOfUpscaledInstanceIsEmpty() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(success);

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE), eq(Boolean.TRUE)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("");
        instanceMetaData.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).containsOnly("upscaled");

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), applyTemplateBodyCatcher.capture(), eq(Boolean.TRUE), eq(Boolean.TRUE));

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTestWhenRackIdUpdatedForOutdatedClusterHostAndUpscaledHost() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(success);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE), eq(Boolean.TRUE)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaDataOriginal = new InstanceMetaData();
        instanceMetaDataOriginal.setDiscoveryFQDN("original");
        instanceMetaDataOriginal.setRackId("/originalRack");
        instanceMetaDataOriginal.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaDataUpscaled = new InstanceMetaData();
        instanceMetaDataUpscaled.setDiscoveryFQDN("upscaled");
        instanceMetaDataUpscaled.setRackId("/upscaledRack");
        instanceMetaDataUpscaled.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaDataOriginal, instanceMetaDataUpscaled);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).containsOnly("original", "upscaled");

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), applyTemplateBodyCatcher.capture(), eq(Boolean.TRUE), eq(Boolean.TRUE));

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi, times(2)).execute(batchRequestCaptor.capture());

        List<ApiBatchRequest> batchRequests = batchRequestCaptor.getAllValues();
        assertThat(batchRequests).hasSize(2);
        verifyRackIdBatch(batchRequests.get(0), "originalId", "/originalRack");
        verifyRackIdBatch(batchRequests.get(1), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterTestWhenRackIdNotUpdatedForClusterHostButSetForUpscaledHost() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true, "/originalRack");
        setUpDeployClientConfigPolling(success);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE), eq(Boolean.TRUE)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);

        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaDataOriginal = new InstanceMetaData();
        instanceMetaDataOriginal.setDiscoveryFQDN("original");
        instanceMetaDataOriginal.setRackId("/originalRack");
        instanceMetaDataOriginal.setInstanceGroup(instanceGroup);
        InstanceMetaData instanceMetaDataUpscaled = new InstanceMetaData();
        instanceMetaDataUpscaled.setDiscoveryFQDN("upscaled");
        instanceMetaDataUpscaled.setRackId("/upscaledRack");
        instanceMetaDataUpscaled.setInstanceGroup(instanceGroup);
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaDataOriginal, instanceMetaDataUpscaled);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(instanceMetaDataList)));

        assertThat(result).containsOnly("upscaled", "original");

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), applyTemplateBodyCatcher.capture(), eq(Boolean.TRUE), eq(Boolean.TRUE));

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndNoPostUpgradeCommandIsAvailable() throws CloudbreakException, ApiException {
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getParcelsResourceApi(any())).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(any())).thenReturn(servicesResourceApi);
        BigDecimal apiCommandId = new BigDecimal(200);

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

        // Start services if they are not running
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        ApiCommandList activeCommandList = new ApiCommandList();
        activeCommandList.setItems(new ArrayList<>());
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_3.getVersion());

        // Restart services
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client)).thenReturn(success);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_3.getVersion());
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
        BigDecimal apiCommandId = new BigDecimal(200);

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

        // Start services if they are not running
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());

        // Restart services
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
        Call call = mock(Call.class);
        when(hostResourceApi.addTagsAsync(eq(HOSTNAME), any(), any())).thenReturn(call);
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
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clustersResourceApi, times(1)).startCommand(STACK_NAME);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, v31Client);
        verify(clouderaManagerRestartService, times(2)).waitForRestartExecutionIfPresent(v31Client, stack, false);
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());
        ArgumentCaptor<List<ApiEntityTag>> entityTagListCaptor = ArgumentCaptor.forClass(List.class);
        verify(hostResourceApi, times(1)).addTagsAsync(eq(HOSTNAME), entityTagListCaptor.capture(), any());
        assertEquals("_cldr_cm_host_template_name", entityTagListCaptor.getValue().get(0).getName());
        assertEquals(GROUP_NAME, entityTagListCaptor.getValue().get(0).getValue());

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
        BigDecimal apiCommandId = new BigDecimal(200);

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

        // Start services if they are not running
        ApiServiceList serviceList = new ApiServiceList();
        ApiService service = new ApiService();
        service.setServiceState(ApiServiceState.STOPPED);
        serviceList.addItemsItem(service);
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(apiCommandId);
        when(servicesResourceApi.readServices(any(), any())).thenReturn(serviceList);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());

        // Restart services
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client)).thenReturn(success);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());
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
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, v31Client);
        verify(clustersResourceApi, times(0)).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());

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

        BigDecimal apiCommandId = new BigDecimal(200);
        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, v31Client, apiCommandId)).thenReturn(success);

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
        when(clouderaManagerCommonCommandService.getDeployClientConfigCommandId(any(), any(), any())).thenReturn(apiCommandId);
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(stack, v31Client, apiCommandId))
                .thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, v31Client, apiCommandId))
                .thenReturn(success);
        when(clouderaManagerPollingServiceProvider.startPollingCmHostStatus(stack, v31Client))
                .thenReturn(success);
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());
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
        verify(clouderaManagerUpgradeService, times(1)).callUpgradeCdhCommand(TestUtil.CDH_VERSION, clustersResourceApi, stack, v31Client, true);
        verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(parcelsResourceApi), eq(stack), eq(v31Client));
        verify(clouderaManagerCommonCommandService, times(1)).getDeployClientConfigCommandId(any(), any(), any());
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
        inOrder.verify(clouderaManagerUpgradeService).callUpgradeCdhCommand(TestUtil.CDH_VERSION, clustersResourceApi, stack, v31Client, true);
        inOrder.verify(servicesResourceApi).readServices(stack.getName(), "SUMMARY");
        inOrder.verify(clouderaManagerCommonCommandService).getDeployClientConfigCommandId(stack, clustersResourceApi, apiCommandList.getItems());
        inOrder.verify(clouderaManagerCommonCommandService).getApiCommand(any(), any(), any(), any());
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
                new ApiCommand().name("DeployClusterClientConfig").id(BigDecimal.ONE),
                new ApiCommand().name("RefreshCluster").id(BigDecimal.ONE));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenReturn(new ApiCommand().id(BigDecimal.ONE));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(v31Client), any());
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
                new ApiCommand().name("DeployClusterClientConfig").id(BigDecimal.ONE),
                new ApiCommand().name("RefreshCluster").id(BigDecimal.ONE));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(v31Client)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenThrow(new ClouderaManagerOperationFailedException("RefreshCommand failed"));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY", null)).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(v31Client), any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenRefreshFailAndForcedIsFalseFail() throws ApiException {
        ApiService apiService = new ApiService().configStalenessStatus(ApiConfigStalenessStatus.STALE)
                .clientConfigStalenessStatus(ApiConfigStalenessStatus.FRESH);
        List<ApiService> apiServices = List.of(apiService);
        ApiServiceList apiServiceList = new ApiServiceList();
        apiServiceList.setItems(apiServices);

        List<ApiCommand> apiCommands = List.of(
                new ApiCommand().name("DeployClusterClientConfig").id(BigDecimal.ONE),
                new ApiCommand().name("RefreshCluster").id(BigDecimal.ONE));
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
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(v31Client), any());
    }

    @Test
    public void testDeployConfigAndRestartClusterServices() throws Exception {
        // GIVEN
        when(clustersResourceApi.deployClientConfig(stack.getName())).thenReturn(new ApiCommand().id(new BigDecimal(100)));
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
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);

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
        apiCommand.setId(new BigDecimal(1));
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
        apiCommand.setId(new BigDecimal(1));
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
        apiCommand.setId(new BigDecimal(1));

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
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
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

    private ClusterComponentView createClusterComponent(ClouderaManagerProduct clouderaManagerProduct) {
        ClusterComponentView component = new ClusterComponentView();
        Json attribute = mock(Json.class);
        when(attribute.getSilent(ClouderaManagerProduct.class)).thenReturn(clouderaManagerProduct);
        component.setAttributes(attribute);
        return component;
    }

    private ClouderaManagerProduct createClouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(name);
        product.setVersion(version);
        return product;
    }

    private void setUpReadHosts(boolean withUpscaled) throws ApiException {
        setUpReadHosts(withUpscaled, null);
    }

    private void setUpReadHosts(boolean withUpscaled, String originalRackId) throws ApiException {
        ApiHostList apiHostsRef;
        ApiHost originalHost = new ApiHost().hostname("original").hostId("originalId").rackId(originalRackId);
        if (withUpscaled) {
            apiHostsRef = new ApiHostList().items(List.of(originalHost, new ApiHost().hostname("upscaled").hostId("upscaledId")));
        } else {
            apiHostsRef = new ApiHostList().items(List.of(originalHost));
        }
        when(hostResourceApi.readHosts(eq(null), eq(null), eq("SUMMARY"))).thenReturn(apiHostsRef);
        when(clouderaManagerApiFactory.getHostsResourceApi(eq(v31Client))).thenReturn(hostResourceApi);
    }

    private void setUpDeployClientConfigPolling(ExtendedPollingResult success) throws ApiException, CloudbreakException {
        BigDecimal deployClientCommandId = new BigDecimal(100);
        when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq("SUMMARY"), eq(null))).thenReturn(new ApiCommandList().addItemsItem(
                new ApiCommand().id(BigDecimal.ONE).name("notDeployClientConfig")));
        when(clouderaManagerCommonCommandService.getDeployClientConfigCommandId(any(), any(), any())).thenReturn(deployClientCommandId);
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(eq(stack), eq(v31Client), eq(deployClientCommandId)))
                .thenReturn(success);
    }

    @Test
    public void testReadServices() {
        List<ApiService> services = List.of(
                new ApiService().name("RANGER_RAZ").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("ATLAS").serviceState(ApiServiceState.STOPPED),
                new ApiService().name("TEZ").serviceState(ApiServiceState.NA),
                new ApiService().name("HDFS").serviceState(ApiServiceState.STOPPING));
        when(clouderaManagerServiceManagementService.readServices(any(), anyString())).thenReturn(new ApiServiceList().items(services));

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
        when(startCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, BigDecimal.ONE)).thenReturn(pollingResult);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        underTest.startCluster(true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, BigDecimal.ONE);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while stopping Cloudera Manager services.");
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
        clusterCommand.setCommandId(BigDecimal.ONE);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(startCommand);
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, BigDecimal.ONE)).thenReturn(pollingResult);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));

        underTest.startCluster(true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, BigDecimal.ONE);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while stopping Cloudera Manager services.");
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
        clusterCommand.setCommandId(BigDecimal.ONE);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(startCommand.isActive()).thenReturn(Boolean.TRUE);
        when(clouderaManagerCommandsService.getApiCommandIfExist(v31Client, BigDecimal.ONE)).thenReturn(Optional.of(startCommand));
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, BigDecimal.ONE)).thenReturn(pollingResult);

        underTest.startCluster(true);

        verify(clustersResourceApi, never()).startCommand(STACK_NAME);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, BigDecimal.ONE);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while stopping Cloudera Manager services.");
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
        clusterCommand.setCommandId(BigDecimal.ONE);
        when(clusterCommandService.findTopByClusterIdAndClusterCommandType(anyLong(), eq(ClusterCommandType.START_CLUSTER)))
                .thenReturn(Optional.of(clusterCommand));
        ApiCommand startCommand = mock(ApiCommand.class);
        when(clouderaManagerCommandsService.getApiCommandIfExist(v31Client, BigDecimal.ONE)).thenReturn(Optional.of(startCommand));
        ExtendedPollingResult pollingResult = mock(ExtendedPollingResult.class);
        ApiCommand newStartCommand = mock(ApiCommand.class);
        when(newStartCommand.getId()).thenReturn(BigDecimal.ONE);
        when(clustersResourceApi.startCommand(STACK_NAME)).thenReturn(newStartCommand);
        when(clusterCommandService.save(any(ClusterCommand.class))).thenAnswer(i -> i.getArgument(0));
        when(clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, v31Client, BigDecimal.ONE)).thenReturn(pollingResult);

        underTest.startCluster(true);

        verify(clustersResourceApi, times(1)).startCommand(STACK_NAME);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, v31Client, BigDecimal.ONE);
        verify(pollingResultErrorHandler, times(1)).handlePollingResult(pollingResult,
                "Cluster was terminated while waiting for Cloudera Runtime services to start",
                "Timeout while stopping Cloudera Manager services.");
        verify(clusterCommandService, times(2)).delete(any(ClusterCommand.class));
    }
}
