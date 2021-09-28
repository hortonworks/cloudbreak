package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_5_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

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
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.cm.util.TestUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

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

    @Spy
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    private Cluster cluster;

    private HostGroup hostGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        stack.setName(STACK_NAME);
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        hostGroup = new HostGroup();
        hostGroup.setName(HOST_GROUP_NAME);
    }

    @Test
    void upscaleClusterListHostsException() throws Exception {
        ApiException apiException = new ApiException("Failed to get hosts");
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenThrow(apiException);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        CloudbreakException exception = assertThrows(CloudbreakException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));

        assertEquals("Failed to upscale", exception.getMessage());
        assertThat(exception).hasCauseReference(apiException);
    }

    @Test
    void upscaleClusterNoHostToUpscale() throws Exception {
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        setUpListClusterHosts();
        setUpReadHosts(false);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTestWhenNoHostToUpscaleButRackIdUpdatedForOutdatedClusterHost() throws Exception {
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        setUpListClusterHosts();
        setUpReadHosts(false);

        setUpBatchSuccess();

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        instanceMetaData.setRackId("/originalRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "originalId", "/originalRack");
    }

    @Test
    void upscaleClusterTestWhenNoHostToUpscaleAndRackIdNotUpdatedForClusterHost() throws Exception {
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        setUpListClusterHosts();
        setUpReadHosts(false, "/originalRack");

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        instanceMetaData.setRackId("/originalRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterRecovery() throws Exception {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerResourceApi.refreshParcelRepos()).thenReturn(new ApiCommand().id(REFRESH_PARCEL_REPOS_ID));
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelRepositoryRefresh(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, apiClientMock, REFRESH_PARCEL_REPOS_ID))
                .thenReturn(PollingResult.SUCCESS);
        setUpListClusterHosts();
        setUpReadHosts(false);
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("original");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(apiClientMock, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTerminationOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        PollingResult pollingResult = PollingResult.EXIT;
        setUpDeployClientConfigPolling(pollingResult);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        String exceptionMessage = "Cluster was terminated while waiting for config deploy";
        doThrow(new CancellationException(exceptionMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        CancellationException exception = assertThrows(CancellationException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));

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
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);
    }

    @Test
    void upscaleClusterTimeoutOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        PollingResult pollingResult = PollingResult.TIMEOUT;
        setUpDeployClientConfigPolling(pollingResult);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        String exceptionMessage = "Timeout while Cloudera Manager was config deploying services.";

        doThrow(new CloudbreakException(exceptionMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());

        CloudbreakException exception = assertThrows(CloudbreakException.class, () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));

        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        verify(clouderaManagerRoleRefreshService, never()).refreshClusterRoles(any(ApiClient.class), any(Stack.class));
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleCluster() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), any(ApiHostRefList.class)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(apiClientMock))).thenReturn(hostTemplatesResourceApi);

        PollingResult applyTemplatePollingResult = PollingResult.SUCCESS;
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(apiClientMock), eq(applyHostTemplateCommandId)))
                .thenReturn(applyTemplatePollingResult);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        assertThat(result).isEqualTo(List.of("upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), applyTemplateBodyCatcher.capture());

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    private void setUpBatchWithResponseAnswer(Answer<ApiBatchResponse> batchResponseAnswer) throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(apiClientMock)).thenReturn(batchResourceApi);
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
                () -> underTest.upscaleCluster(hostGroup, instanceMetaDataList));

        assertThat(exception).hasMessageStartingWith("Setting rack ID for hosts batch operation failed. Response: ");

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService, never()).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        verify(hostTemplatesResourceApi, never()).applyHostTemplate(anyString(), anyString(), anyBoolean(), any(ApiHostRefList.class));

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterTestWhenRackIdOfUpscaledInstanceIsEmpty() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
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
        instanceMetaData.setRackId("");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);

        List<String> result = underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        assertThat(result).isEqualTo(List.of("upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), applyTemplateBodyCatcher.capture());

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTestWhenRackIdUpdatedForOutdatedClusterHostAndUpscaledHost() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), any(ApiHostRefList.class)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(apiClientMock))).thenReturn(hostTemplatesResourceApi);

        PollingResult applyTemplatePollingResult = PollingResult.SUCCESS;
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(apiClientMock), eq(applyHostTemplateCommandId)))
                .thenReturn(applyTemplatePollingResult);

        InstanceMetaData instanceMetaDataOriginal = new InstanceMetaData();
        instanceMetaDataOriginal.setDiscoveryFQDN("original");
        instanceMetaDataOriginal.setRackId("/originalRack");
        InstanceMetaData instanceMetaDataUpscaled = new InstanceMetaData();
        instanceMetaDataUpscaled.setDiscoveryFQDN("upscaled");
        instanceMetaDataUpscaled.setRackId("/upscaledRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaDataOriginal, instanceMetaDataUpscaled);

        List<String> result = underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        assertThat(result).isEqualTo(List.of("original", "upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), applyTemplateBodyCatcher.capture());

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
        setUpDeployClientConfigPolling(PollingResult.SUCCESS);

        setUpBatchSuccess();

        BigDecimal applyHostTemplateCommandId = new BigDecimal(200);
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), any(ApiHostRefList.class)))
                .thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(apiClientMock))).thenReturn(hostTemplatesResourceApi);

        PollingResult applyTemplatePollingResult = PollingResult.SUCCESS;
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(apiClientMock), eq(applyHostTemplateCommandId)))
                .thenReturn(applyTemplatePollingResult);

        InstanceMetaData instanceMetaDataOriginal = new InstanceMetaData();
        instanceMetaDataOriginal.setDiscoveryFQDN("original");
        instanceMetaDataOriginal.setRackId("/originalRack");
        InstanceMetaData instanceMetaDataUpscaled = new InstanceMetaData();
        instanceMetaDataUpscaled.setDiscoveryFQDN("upscaled");
        instanceMetaDataUpscaled.setRackId("/upscaledRack");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaDataOriginal, instanceMetaDataUpscaled);

        List<String> result = underTest.upscaleCluster(hostGroup, instanceMetaDataList);

        assertThat(result).isEqualTo(List.of("original", "upscaled"));

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(any(ApiClient.class), any(Stack.class));

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiHostRefList> applyTemplateBodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(hostTemplatesResourceApi, times(1))
                .applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), eq(Boolean.TRUE), applyTemplateBodyCatcher.capture());

        assertEquals(1, applyTemplateBodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", applyTemplateBodyCatcher.getValue().getItems().get(0).getHostname());

        ArgumentCaptor<ApiBatchRequest> batchRequestCaptor = ArgumentCaptor.forClass(ApiBatchRequest.class);
        verify(batchResourceApi).execute(batchRequestCaptor.capture());

        verifyRackIdBatch(batchRequestCaptor.getValue(), "upscaledId", "/upscaledRack");
    }

    @Test
    void testUpgradeClusterComponentIsNotPresent() throws ApiException {
        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        Set<ClusterComponent> clusterComponents = TestUtil.clusterComponentSet(cluster);
        Set<ClusterComponent> clusterComponentsNoCDH = clusterComponents.stream().filter(clusterComponent -> !clusterComponent.getName().equals("CDH"))
                .collect(Collectors.toSet());

        cluster.setComponents(clusterComponentsNoCDH);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.upgradeClusterRuntime(clusterComponentsNoCDH, false));
        Assertions.assertEquals("Runtime component not found!", exception.getMessage());
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndNoPostUpgradeCommandIsAvailable() throws CloudbreakException, ApiException {
        TestUtil.clusterComponents(cluster);

        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_4_3.getVersion());

        // Restart services
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(new ApiCommandList().items(List.of()));
        when(clustersResourceApi.restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class))).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        underTest.upgradeClusterRuntime(cluster.getComponents(), true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, apiClientMock);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerPollingServiceProvider, times(2)).startPollingCmServicesRestart(stack, apiClientMock, apiCommandId);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clustersResourceApi, times(1)).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
        verifyNoInteractions(clouderaManagerUpgradeService);

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clustersResourceApi);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClientMock);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clustersResourceApi).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndPostUpgradeCommandIsAvailable()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        TestUtil.clusterComponents(cluster);

        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());

        // Restart services
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(new ApiCommandList().items(List.of()));
        when(clustersResourceApi.restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class))).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        when(clouderaManagerApiClientProvider.getV45Client(any(), any(), any(), any())).thenReturn(apiClientMock);

        underTest.upgradeClusterRuntime(cluster.getComponents(), true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, apiClientMock);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerPollingServiceProvider, times(2)).startPollingCmServicesRestart(stack, apiClientMock, apiCommandId);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, apiClientMock);
        verify(clustersResourceApi, times(1)).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
        verify(clustersResourceApi, times(2)).listActiveCommands(stack.getName(), "SUMMARY");
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService,
                clustersResourceApi, clouderaManagerUpgradeService, clouderaManagerApiClientProvider);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClientMock);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clustersResourceApi).listActiveCommands(eq(stack.getName()), any());
        inOrder.verify(clouderaManagerApiClientProvider).getV45Client(any(), any(), any(), any());
        inOrder.verify(clouderaManagerUpgradeService).callPostRuntimeUpgradeCommand(eq(clustersResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clustersResourceApi).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
    }

    @Test
    void testUpgradeClusterWhenPatchUpgradeAndPostUpgradeCommandIsAvailableAndRestartIsRunning()
            throws CloudbreakException, ApiException, ClouderaManagerClientInitException {
        TestUtil.clusterComponents(cluster);

        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        // Post parcel activation
        ClouderaManagerRepo clouderaManagerRepo = mock(ClouderaManagerRepo.class);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(clouderaManagerRepo);
        when(clouderaManagerRepo.getVersion()).thenReturn(CLOUDERAMANAGER_VERSION_7_5_1.getVersion());

        // Restart services
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(new ApiCommandList().items(
                List.of(new ApiCommand().id(apiCommandId).name("Restart"))));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

        when(clouderaManagerApiClientProvider.getV45Client(any(), any(), any(), any())).thenReturn(apiClientMock);

        underTest.upgradeClusterRuntime(cluster.getComponents(), true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, apiClientMock);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        verify(mgmtServiceResourceApi, times(1)).listActiveCommands("SUMMARY");
        verify(mgmtServiceResourceApi, times(1)).restartCommand();
        verify(clouderaManagerPollingServiceProvider, times(3)).startPollingCmServicesRestart(stack, apiClientMock, apiCommandId);
        verify(clouderaManagerParcelManagementService, times(1)).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(1)).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(eventService, times(1)).fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_START_POST_UPGRADE);
        verify(clouderaManagerUpgradeService, times(1)).callPostRuntimeUpgradeCommand(clustersResourceApi, stack, apiClientMock);
        verify(clustersResourceApi, times(0)).restartCommand(eq(stack.getName()), any(ApiRestartClusterArgs.class));
        verify(clustersResourceApi, times(2)).listActiveCommands(stack.getName(), "SUMMARY");
        verify(clouderaManagerApiClientProvider, times(1)).getV45Client(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clustersResourceApi,
                clouderaManagerApiClientProvider, clouderaManagerUpgradeService);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClientMock);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clustersResourceApi).listActiveCommands(eq(stack.getName()), any());
        inOrder.verify(clouderaManagerApiClientProvider).getV45Client(any(), any(), any(), any());
        inOrder.verify(clouderaManagerUpgradeService).callPostRuntimeUpgradeCommand(eq(clustersResourceApi), eq(stack), eq(apiClientMock));
    }

    @Test
    void testUpgradeClusterWhenNotPatchUpgrade() throws CloudbreakException, ApiException {
        TestUtil.clusterComponents(cluster);

        when(clouderaManagerApiFactory.getMgmtServiceResourceApi(any())).thenReturn(mgmtServiceResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(any())).thenReturn(parcelResourceApi);
        when(clouderaManagerApiFactory.getClustersResourceApi(any())).thenReturn(clustersResourceApi);
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(clouderaManagerResourceApi);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);

        BigDecimal apiCommandId = new BigDecimal(200);
        PollingResult successPollingResult = PollingResult.SUCCESS;

        // Mgmt Service restart
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(new ArrayList<>());
        when(mgmtServiceResourceApi.listActiveCommands("SUMMARY")).thenReturn(apiCommandList);
        when(mgmtServiceResourceApi.restartCommand()).thenReturn(new ApiCommand().id(apiCommandId));
        when(clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClientMock, apiCommandId)).thenReturn(successPollingResult);

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
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);

        when(clouderaManagerCommonCommandService.getDeployClientConfigCommandId(any(), any(), any())).thenReturn(apiCommandId);
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, apiClientMock, apiCommandId))
                .thenReturn(successPollingResult);

        underTest.upgradeClusterRuntime(cluster.getComponents(), false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmStartup(stack, apiClientMock);
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmHostStatus(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).checkParcelApiAvailability(stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(1)).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        verify(clouderaManagerParcelManagementService, times(1)).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        verify(clouderaManagerParcelManagementService, times(2)).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerParcelManagementService, times(2)).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerUpgradeService, times(1)).callUpgradeCdhCommand(TestUtil.CDH_VERSION, clustersResourceApi, stack, apiClientMock);
        verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        verify(clouderaManagerCommonCommandService, times(1)).getDeployClientConfigCommandId(any(), any(), any());
        verify(clouderaManagerCommonCommandService, times(1)).getApiCommand(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(clouderaManagerPollingServiceProvider, clouderaManagerParcelManagementService, clouderaManagerUpgradeService,
                clustersResourceApi, clouderaManagerCommonCommandService);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmStartup(stack, apiClientMock);
        inOrder.verify(clouderaManagerPollingServiceProvider).startPollingCmHostStatus(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).checkParcelApiAvailability(stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).setParcelRepos(any(), eq(clouderaManagerResourceApi));
        inOrder.verify(clouderaManagerParcelManagementService).refreshParcelRepos(clouderaManagerResourceApi, stack, apiClientMock);
        inOrder.verify(clouderaManagerParcelManagementService).downloadParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).distributeParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerParcelManagementService).activateParcels(any(), eq(parcelResourceApi), eq(stack), eq(apiClientMock));
        inOrder.verify(clouderaManagerUpgradeService).callUpgradeCdhCommand(TestUtil.CDH_VERSION, clustersResourceApi, stack, apiClientMock);
        inOrder.verify(clouderaManagerCommonCommandService).getDeployClientConfigCommandId(stack, clustersResourceApi, apiCommandList.getItems());
        inOrder.verify(clouderaManagerCommonCommandService).getApiCommand(any(), any(), any(), any());
    }

    @Test
    void testPollRefreshWhenCancelled() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        PollingResult pollingResult = PollingResult.EXIT;
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(pollingResult);
        doThrow(new CancellationException("Cluster was terminated while waiting for service refresh")).when(pollingResultErrorHandler)
                .handlePollingResult(eq(pollingResult), any(), any());
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals("Cluster was terminated while waiting for service refresh", actual.getMessage());
    }

    @Test
    void testPollRefreshWhenTimeout() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        PollingResult pollingResult = PollingResult.TIMEOUT;
        String expectedMessage = "Timeout while Cloudera Manager was refreshing services.";
        when(clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(any(), any(), any())).thenReturn(pollingResult);
        doThrow(new CloudbreakException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());
        CloudbreakException actual = assertThrows(CloudbreakException.class, () -> underTest.pollRefresh(apiCommand));
        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenCancelled() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        PollingResult pollingResult = PollingResult.EXIT;
        String expectedMessage = "Cluster was terminated while waiting for config deploy";
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(pollingResult);
        doThrow(new CancellationException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());
        CancellationException actual = assertThrows(CancellationException.class, () -> underTest.pollDeployConfig(apiCommand.getId()));
        assertEquals(expectedMessage, actual.getMessage());
    }

    @Test
    void testPollDeployConfigWhenTimeout() throws CloudbreakException {
        ApiCommand apiCommand = new ApiCommand();
        PollingResult pollingResult = PollingResult.TIMEOUT;
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(any(), any(), any())).thenReturn(pollingResult);
        String expectedMessage = "Timeout while Cloudera Manager was config deploying services.";
        doThrow(new CloudbreakException(expectedMessage)).when(pollingResultErrorHandler).handlePollingResult(eq(pollingResult), any(), any());
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

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);

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
                new ApiCommand().name("RefreshCluster").id(BigDecimal.ONE));
        ApiCommandList apiCommandList = new ApiCommandList();
        apiCommandList.setItems(apiCommands);

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenReturn(new ApiCommand().id(BigDecimal.ONE));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), any());
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmConfigurationRefresh(eq(stack), eq(apiClientMock), any());
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

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenThrow(new ClouderaManagerOperationFailedException("RefreshCommand failed"));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);

        underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, true);

        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), any());
    }

    @Test
    void testDeployConfigAndRefreshCMStaleServicesWhenRefreshFailAndForcedIsFalseFail() throws CloudbreakException, ApiException {
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

        when(clouderaManagerApiFactory.getServicesResourceApi(apiClientMock)).thenReturn(servicesResourceApi);
        when(clouderaManagerCommonCommandService.getApiCommand(any(), any(), any(), any()))
                .thenThrow(new ClouderaManagerOperationFailedException("RefreshCommand failed"));
        when(servicesResourceApi.readServices("stack_name", "SUMMARY")).thenReturn(apiServiceList);
        when(clustersResourceApi.listActiveCommands(stack.getName(), "SUMMARY")).thenReturn(apiCommandList);

        ClouderaManagerOperationFailedException exception = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.deployConfigAndRefreshCMStaleServices(clustersResourceApi, false));

        assertEquals("RefreshCommand failed", exception.getMessage());
        verify(clouderaManagerPollingServiceProvider, times(1)).startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), any());
    }

    @Test
    public void removeUnusedParcels() {
        // GIVEN
        ClouderaManagerProduct cmProduct1 = createClouderaManagerProduct("product1", "version1");
        ClouderaManagerProduct cmProduct2 = createClouderaManagerProduct("product2", "version2");
        Set<ClusterComponent> usedComponents = Set.of(createClusterComponent(cmProduct1), createClusterComponent(cmProduct2));
        Map<String, ClouderaManagerProduct> productMap = Map.of(cmProduct1.getName(), cmProduct1, cmProduct2.getName(), cmProduct2);
        when(clouderaManagerApiFactory.getParcelsResourceApi(apiClientMock)).thenReturn(parcelsResourceApi);
        when(clouderaManagerApiFactory.getParcelResourceApi(apiClientMock)).thenReturn(parcelResourceApi);
        // WHEN
        underTest.removeUnusedParcels(usedComponents);
        // THEN
        verify(clouderaManagerParcelDecommissionService, times(1)).deactivateUnusedParcels(parcelsResourceApi, parcelResourceApi, stack.getName(), productMap);
        verify(clouderaManagerParcelDecommissionService, times(1)).undistributeUnusedParcels(apiClientMock, parcelsResourceApi, parcelResourceApi, stack,
                productMap);
        verify(clouderaManagerParcelDecommissionService, times(1)).removeUnusedParcels(apiClientMock, parcelsResourceApi, parcelResourceApi, stack, productMap);
    }

    private ClusterComponent createClusterComponent(ClouderaManagerProduct clouderaManagerProduct) {
        ClusterComponent component = new ClusterComponent();
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
        when(clouderaManagerApiFactory.getHostsResourceApi(eq(apiClientMock))).thenReturn(hostResourceApi);
    }

    private void setUpDeployClientConfigPolling(PollingResult success) throws ApiException, CloudbreakException {
        BigDecimal deployClientCommandId = new BigDecimal(100);
        when(clustersResourceApi.listActiveCommands(STACK_NAME, "SUMMARY")).thenReturn(new ApiCommandList().addItemsItem(
                new ApiCommand().id(BigDecimal.ONE).name("notDeployClientConfig")));
        when(clouderaManagerCommonCommandService.getDeployClientConfigCommandId(any(), any(), any())).thenReturn(deployClientCommandId);
        when(clouderaManagerPollingServiceProvider.startPollingCmClientConfigDeployment(eq(stack), eq(apiClientMock), eq(deployClientCommandId)))
                .thenReturn(success);
    }
}
