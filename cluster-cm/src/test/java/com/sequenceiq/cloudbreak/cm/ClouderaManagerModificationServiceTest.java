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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostTemplatesResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerModificationServiceTest {

    private static final String STACK_NAME = "stack_name";

    private static final String HOST_GROUP_NAME = "host_group_name";

    @InjectMocks
    private ClouderaManagerModificationService underTest;

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

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

    private Cluster cluster;

    private HostGroup hostGroup;

    private List<InstanceMetaData> instaneMetadata;

    private List<HostMetadata> hostMetadataList;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        stack.setName(STACK_NAME);
        stack.setCluster(cluster);
        cluster = new Cluster();
        hostMetadataList = createHmListWithUpscaledHost();
        hostGroup = new HostGroup();
        hostGroup.setName(HOST_GROUP_NAME);
        instaneMetadata = List.of(new InstanceMetaData());
    }

    @Test
    void upscaleClusterListHostsException() throws Exception {
        HostMetadata hostMetadata = new HostMetadata();

        when(clustersResourceApi.listHosts(eq(STACK_NAME))).thenThrow(new ApiException("Failed to get hosts"));
        when(clouderaManagerClientFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);

        Exception exception = assertThrows(CloudbreakException.class, () ->
                underTest.upscaleCluster(hostGroup, List.of(hostMetadata), instaneMetadata));
        assertEquals("Failed to upscale", exception.getMessage());
    }

    @Test
    void upscaleClusterNoHostToUpscale() throws Exception {
        HostMetadata originalHm = new HostMetadata();
        originalHm.setHostName("original");

        setUpListClusterHosts();

        underTest.upscaleCluster(hostGroup, List.of(originalHm), instaneMetadata);
        verify(clouderaManagerClientFactory, never()).getHostsResourceApi(any());
    }

    @Test
    void upscaleClusterTerminationOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts();
        setUpDeployClientConfigPolling(PollingResult.EXIT);

        Exception exception = assertThrows(CancellationException.class, () ->
                underTest.upscaleCluster(hostGroup, hostMetadataList, instaneMetadata));
        String exceptionMessage = "Cluster was terminated while waiting for client configurations to deploy";
        assertEquals(exceptionMessage, exception.getMessage());

        ArgumentCaptor<ApiHostRefList> bodyCatcher = ArgumentCaptor.forClass(ApiHostRefList.class);
        verify(clustersResourceApi, times(1)).addHosts(eq(STACK_NAME), bodyCatcher.capture());

        assertEquals(1, bodyCatcher.getValue().getItems().size());
        assertEquals("upscaled", bodyCatcher.getValue().getItems().get(0).getHostname());
    }

    private void setUpListClusterHosts() throws ApiException {
        ApiHostRefList clusterHostsRef = new ApiHostRefList().items(List.of(new ApiHostRef().hostname("original")));
        when(clustersResourceApi.listHosts(eq(STACK_NAME))).thenReturn(clusterHostsRef);
        when(clouderaManagerClientFactory.getClustersResourceApi(eq(apiClientMock))).thenReturn(clustersResourceApi);
    }

    private List<HostMetadata> createHmListWithUpscaledHost() {
        HostMetadata originalHm = new HostMetadata();
        originalHm.setHostName("original");
        HostMetadata upscaledHm = new HostMetadata();
        upscaledHm.setHostName("upscaled");
        return List.of(originalHm, upscaledHm);
    }

    @Test
    void upscaleClusterTimeoutOnDeployConfig() throws Exception {
        setUpListClusterHosts();
        setUpReadHosts();
        setUpDeployClientConfigPolling(PollingResult.TIMEOUT);

        Exception exception = assertThrows(CloudbreakException.class, () ->
                underTest.upscaleCluster(hostGroup, hostMetadataList, instaneMetadata));
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
        when(clouderaManagerClientFactory.getHostTemplatesResourceApi(eq(apiClientMock))).thenReturn(hostTemplatesResourceApi);

        PollingResult applyTemplatePollingResult = PollingResult.SUCCESS;
        when(clouderaManagerPollingServiceProvider.applyHostTemplatePollingService(eq(stack), eq(apiClientMock), eq(applyHostTemplateCommandId)))
                .thenReturn(applyTemplatePollingResult);

        underTest.upscaleCluster(hostGroup, hostMetadataList, instaneMetadata);

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

    private void setUpReadHosts() throws ApiException {
        ApiHostList apiHostsRef = new ApiHostList().items(
                List.of(new ApiHost().hostname("original"), new ApiHost().hostname("upscaled"))
        );
        when(hostResourceApi.readHosts(eq("SUMMARY"))).thenReturn(apiHostsRef);
        when(clouderaManagerClientFactory.getHostsResourceApi(eq(apiClientMock))).thenReturn(hostResourceApi);
    }

    private void setUpDeployClientConfigPolling(PollingResult success) throws ApiException {
        BigDecimal deployClientCommandId = new BigDecimal(100);
        when(clustersResourceApi.deployClientConfig(eq(STACK_NAME))).thenReturn(new ApiCommand().id(deployClientCommandId));

        PollingResult deployPollingResult = success;
        when(clouderaManagerPollingServiceProvider.deployClientConfigPollingService(eq(stack), eq(apiClientMock), eq(deployClientCommandId)))
                .thenReturn(deployPollingResult);
    }
}