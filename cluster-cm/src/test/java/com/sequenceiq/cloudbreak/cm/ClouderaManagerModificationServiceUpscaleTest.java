package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class ClouderaManagerModificationServiceUpscaleTest extends ClouderaManagerModificationServiceTestBase {

    static Object[][] upscaleClusterTestWhenRackIdBatchExecutionFailureDataProvider() {
        return new Object[][]{
                {"response=null", (Supplier<ApiBatchResponse>) () -> null},
                {"success=null", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(null).items(List.of())},
                {"items=null", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(true).items(null)},
                {"success=false", (Supplier<ApiBatchResponse>) () -> new ApiBatchResponse().success(false).items(List.of())},
        };
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
        when(clouderaManagerPollingServiceProvider.startPollingCmParcelActivation(stack, v31Client, REFRESH_PARCEL_REPOS_ID, java.util.Collections.emptyList()))
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

        verify(clustersResourceApi, never()).addHosts(anyString(), any(ApiHostRefList.class));
        verify(clouderaManagerRoleRefreshService).refreshClusterRoles(v31Client, stack);
        verify(clouderaManagerApiFactory, never()).getBatchResourceApi(any(ApiClient.class));
    }

    @Test
    void upscaleClusterTimeoutOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts(true);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        List<InstanceMetaData> instanceMetaDataList = List.of(instanceMetaData);
        String exceptionMessage = "Timeout while Cloudera Manager was config deploying services.";

        org.mockito.Mockito.doThrow(new CloudbreakException(exceptionMessage)).when(clouderaManagerClientConfigDeployService).deployClientConfig(any());

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
        setUpBatchSuccess();
        ReflectionTestUtils.setField(underTest, "v52Client", null);

        Long applyHostTemplateCommandId = 200L;
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

        setUpBatchSuccess();

        Long applyHostTemplateCommandId = 200L;
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE),
                eq(Boolean.TRUE))).thenReturn(new ApiCommand().id(applyHostTemplateCommandId));
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);

        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(applyHostTemplateCommandId)))
                .thenReturn(success);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
        doNothing().when(clouderaManagerClientConfigDeployService)
                .deployAndPollClientConfig(any());

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

        Long applyHostTemplateCommandId = 200L;
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

        setUpBatchSuccess();

        Long applyHostTemplateCommandId = 200L;
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

        setUpBatchSuccess();

        Long applyHostTemplateCommandId = 200L;
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
        verify(batchResourceApi, times(1)).execute(batchRequestCaptor.capture());

        List<ApiBatchRequest> batchRequests = batchRequestCaptor.getAllValues();
        assertThat(batchRequests).hasSize(1);
        verifyRackIdBatch(batchRequests.get(0), "upscaledId", "/upscaledRack");
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsDuplicateTagErrorAndActiveCommandFound() throws Exception {
        Long activeCommandId = 300L;
        ApiException duplicateTagException = new ApiException(
                400, Collections.emptyMap(),
                "{\"message\":\"duplicate key value violates unique constraint \\\"idx_tags_to_entity\\\"\"}"
        );

        mockApplyHostTemplateCalls(duplicateTagException);

        ApiCommandList activeCommands = new ApiCommandList().items(List.of(
                new ApiCommand().name("ApplyHostTemplate").id(activeCommandId)
        ));
        when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq(SUMMARY), eq(null))).thenReturn(activeCommands);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(activeCommandId)))
                .thenReturn(success);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd()))));

        assertThat(result).isEqualTo(List.of("upscaled"));
        verify(clouderaManagerPollingServiceProvider).startPollingCmApplyHostTemplate(stack, v31Client, activeCommandId);
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsDuplicateTagErrorAndNoActiveCommandFound() throws Exception {
        ApiException duplicateTagException = new ApiException(
                400, Collections.emptyMap(),
                "{\"message\":\"duplicate key value violates unique constraint \\\"idx_tags_to_entity\\\"\"}"
        );

        mockApplyHostTemplateCalls(duplicateTagException);

        ApiCommandList activeCommands = new ApiCommandList().items(List.of(
                new ApiCommand().name("SomeOtherCommand").id(999L)
        ));
        when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq(SUMMARY), eq(null))).thenReturn(activeCommands);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd()))));

        assertThat(result).isEqualTo(List.of("upscaled"));
        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmApplyHostTemplate(any(), any(), any());
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsDuplicateTagErrorWithHostTemplateNameTag() throws Exception {
        Long activeCommandId = 300L;
        ApiException duplicateTagException = new ApiException(
                400, Collections.emptyMap(),
                "{\"message\":\"duplicate key value violates unique constraint: _cldr_cm_host_template_name already exists\"}"
        );

        mockApplyHostTemplateCalls(duplicateTagException);

        ApiCommandList activeCommands = new ApiCommandList().items(List.of(
                new ApiCommand().name("ApplyHostTemplate").id(activeCommandId)
        ));
        when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq(SUMMARY), eq(null))).thenReturn(activeCommands);
        when(clouderaManagerPollingServiceProvider.startPollingCmApplyHostTemplate(eq(stack), eq(v31Client), eq(activeCommandId)))
                .thenReturn(success);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd()))));

        assertThat(result).isEqualTo(List.of("upscaled"));
        verify(clouderaManagerPollingServiceProvider).startPollingCmApplyHostTemplate(stack, v31Client, activeCommandId);
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsNonDuplicateTag400ErrorThenThrows() throws Exception {
        ApiException otherBadRequestException = new ApiException(
                400, Collections.emptyMap(),
                "{\"message\":\"some other bad request error\"}"
        );

        mockApplyHostTemplateCalls(otherBadRequestException);

        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd())))));

        assertEquals("Failed to upscale", exception.getMessage());
        assertThat(exception).hasCauseReference(otherBadRequestException);
        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmApplyHostTemplate(any(), any(), any());
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsNon400ErrorThenThrows() throws Exception {
        ApiException serverErrorException = new ApiException(
                500, Collections.emptyMap(),
                "{\"message\":\"internal server error\"}"
        );

        mockApplyHostTemplateCalls(serverErrorException);

        CloudbreakException exception = assertThrows(CloudbreakException.class,
                () -> underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd())))));

        assertEquals("Failed to upscale", exception.getMessage());
        assertThat(exception).hasCauseReference(serverErrorException);
        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmApplyHostTemplate(any(), any(), any());
    }

    @Test
    void upscaleClusterWhenApplyHostTemplateHitsDuplicateTagErrorAndListActiveCommandsReturnsNull() throws Exception {
        ApiException duplicateTagException = new ApiException(
                400, Collections.emptyMap(),
                "{\"message\":\"duplicate key value violates unique constraint \\\"idx_tags_to_entity\\\"\"}"
        );

        mockApplyHostTemplateCalls(duplicateTagException);

        when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq(SUMMARY), eq(null))).thenReturn(null);

        List<String> result = underTest.upscaleCluster(Map.of(hostGroup, new LinkedHashSet<>(List.of(getUpscaledImd()))));

        assertThat(result).isEqualTo(List.of("upscaled"));
        verify(clouderaManagerPollingServiceProvider, never()).startPollingCmApplyHostTemplate(any(), any(), any());
    }

    private void mockApplyHostTemplateCalls(ApiException duplicateTagException) throws ApiException {
        setUpListClusterHosts();
        setUpReadHosts(true);
        setUpBatchSuccess();
        when(hostTemplatesResourceApi.applyHostTemplate(eq(STACK_NAME), eq(HOST_GROUP_NAME), any(ApiHostRefList.class), eq(Boolean.TRUE), eq(Boolean.TRUE)))
                .thenThrow(duplicateTagException);
        when(clouderaManagerApiFactory.getHostTemplatesResourceApi(eq(v52Client))).thenReturn(hostTemplatesResourceApi);
        when(clouderaManagerRepo.getVersion()).thenReturn("7.10.0");
        when(clusterComponentProvider.getClouderaManagerRepoDetails(anyLong())).thenReturn(clouderaManagerRepo);
    }

    private InstanceMetaData getUpscaledImd() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(HOST_GROUP_NAME);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("upscaled");
        instanceMetaData.setRackId("/upscaledRack");
        instanceMetaData.setInstanceGroup(instanceGroup);
        return instanceMetaData;
    }
}
