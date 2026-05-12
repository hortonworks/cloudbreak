package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBatchRequest;
import com.cloudera.api.swagger.model.ApiBatchRequestElement;
import com.cloudera.api.swagger.model.ApiBatchResponse;
import com.cloudera.api.swagger.model.ApiBatchResponseElement;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.HTTPMethod;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.config.ClouderaManagerFlinkConfigurationService;
import com.sequenceiq.cloudbreak.cm.config.modification.ClouderaManagerConfigModificationService;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
abstract class ClouderaManagerModificationServiceTestBase {

    protected static final String STACK_NAME = "stack_name";

    protected static final String HOST_GROUP_NAME = "host_group_name";

    protected static final long CLUSTER_ID = 1L;

    protected static final Long REFRESH_PARCEL_REPOS_ID = 1L;

    protected static final String HOSTNAME = "host1";

    protected static final String GROUP_NAME = "group1";

    protected static final String ZOOKEEPER_SERVICE_NAME = "ZOOKEEPER";

    protected static final String KAFKA_SERVICE_NAME = "KAFKA";

    protected final ExtendedPollingResult success = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();

    protected final ExtendedPollingResult exit = new ExtendedPollingResult.ExtendedPollingResultBuilder().exit().build();

    protected final ExtendedPollingResult timeout = new ExtendedPollingResult.ExtendedPollingResultBuilder().timeout().build();

    @InjectMocks
    protected ClouderaManagerModificationService underTest;

    @Mock
    protected ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    protected ApiClient v31Client;

    @Mock
    protected ApiClient v52Client;

    @Spy
    protected Stack stack;

    @Mock
    protected HttpClientConfig clientConfig;

    @Mock
    protected ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    protected ClustersResourceApi clustersResourceApi;

    @Mock
    protected HostsResourceApi hostResourceApi;

    @Mock
    protected HostTemplatesResourceApi hostTemplatesResourceApi;

    @Mock
    protected MgmtServiceResourceApi mgmtServiceResourceApi;

    @Mock
    protected ClouderaManagerClientConfigDeployService clouderaManagerClientConfigDeployService;

    @Mock
    protected ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    protected ServicesResourceApi servicesResourceApi;

    @Mock
    protected HostsResourceApi hostsResourceApi;

    @Mock
    protected ParcelResourceApi parcelResourceApi;

    @Mock
    protected ParcelsResourceApi parcelsResourceApi;

    @Mock
    protected BatchResourceApi batchResourceApi;

    @Mock
    protected ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    protected ClouderaManagerRepo clouderaManagerRepo;

    @Mock
    protected ClouderaManagerRoleRefreshService clouderaManagerRoleRefreshService;

    @Mock
    protected CloudbreakEventService eventService;

    @Mock
    protected ClouderaManagerConfigService configService;

    @Mock
    protected ClouderaManagerParcelDecommissionService clouderaManagerParcelDecommissionService;

    @Mock
    protected ClouderaManagerParcelManagementService clouderaManagerParcelManagementService;

    @Mock
    protected ClouderaManagerUpgradeService clouderaManagerUpgradeService;

    @Mock
    protected PollingResultErrorHandler pollingResultErrorHandler;

    @Mock
    protected ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    @Mock
    protected ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    protected ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    protected ClouderaManagerRestartService clouderaManagerRestartService;

    @Mock
    protected ClouderaManagerConfigModificationService clouderaManagerConfigModificationService;

    protected ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    @Mock
    protected ClusterCommandService clusterCommandService;

    @Mock
    protected ClouderaManagerCommandsService clouderaManagerCommandsService;

    @Spy
    protected ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    protected ClouderaManagerFlinkConfigurationService clouderaManagerFlinkConfigurationService;

    @Mock
    protected ClouderaManagerKraftMigrationService clouderaManagerKraftMigrationService;

    @Mock
    protected RolesResourceApi rolesResourceApi;

    protected Cluster cluster;

    protected HostGroup hostGroup;

    @BeforeEach
    void baseSetUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        stack.setName(STACK_NAME);
        cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setName(STACK_NAME);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:cloudbreak:us-west-1:someone:stack:12345");
        hostGroup = new HostGroup();
        hostGroup.setName(HOST_GROUP_NAME);

        clouderaManagerServiceManagementService = spy(new ClouderaManagerServiceManagementService());
        ReflectionTestUtils.setField(clouderaManagerServiceManagementService, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(clouderaManagerServiceManagementService, "clouderaManagerPollingServiceProvider", clouderaManagerPollingServiceProvider);
        ReflectionTestUtils.setField(clouderaManagerServiceManagementService, "pollingResultErrorHandler", pollingResultErrorHandler);
        ReflectionTestUtils.setField(clouderaManagerServiceManagementService, "clusterCommandService", clusterCommandService);
        ReflectionTestUtils.setField(clouderaManagerServiceManagementService, "clouderaManagerCommandsService", clouderaManagerCommandsService);
        ReflectionTestUtils.setField(underTest, "clouderaManagerServiceManagementService", clouderaManagerServiceManagementService);

        lenient().when(clustersResourceApi.listActiveCommands(eq(STACK_NAME), eq(SUMMARY), isNull()))
                .thenReturn(new ApiCommandList().items(List.of()));
        lenient().when(clouderaManagerApiFactory.getClustersResourceApi(v31Client)).thenReturn(clustersResourceApi);
    }

    protected ClusterComponentView createClusterComponent(ClouderaManagerProduct clouderaManagerProduct) {
        ClusterComponentView component = new ClusterComponentView();
        Json attribute = org.mockito.Mockito.mock(Json.class);
        when(attribute.getUnchecked(ClouderaManagerProduct.class)).thenReturn(clouderaManagerProduct);
        component.setAttributes(attribute);
        return component;
    }

    protected ClouderaManagerProduct createClouderaManagerProduct(String name, String version) {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(name);
        product.setVersion(version);
        return product;
    }

    protected void setUpReadHosts(boolean withUpscaled) throws ApiException {
        setUpReadHosts(withUpscaled, null);
    }

    protected void setUpReadHosts(boolean withUpscaled, String originalRackId) throws ApiException {
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

    protected void setUpListClusterHosts() throws ApiException {
        ApiHostList clusterHostsRef = new ApiHostList().items(List.of(new ApiHost().hostname("original")));
        when(clustersResourceApi.listHosts(eq(STACK_NAME), eq(null), eq(null), eq(null))).thenReturn(clusterHostsRef);
        when(clouderaManagerApiFactory.getClustersResourceApi(eq(v31Client))).thenReturn(clustersResourceApi);
    }

    protected void setUpBatchWithResponseAnswer(Answer<ApiBatchResponse> batchResponseAnswer) throws ApiException {
        when(clouderaManagerApiFactory.getBatchResourceApi(v31Client)).thenReturn(batchResourceApi);
        when(batchResourceApi.execute(any(ApiBatchRequest.class))).thenAnswer(batchResponseAnswer);
    }

    protected void setUpBatchSuccess() throws ApiException {
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

    protected void verifyRackIdBatch(ApiBatchRequest batchRequest, String hostIdExpected, String rackIdExpected) {
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
}
