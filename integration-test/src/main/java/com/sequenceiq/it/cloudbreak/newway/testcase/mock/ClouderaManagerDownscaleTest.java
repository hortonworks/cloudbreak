package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.mock.model.ClouderaManagerMock.API_ROOT;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.newway.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.spark.DynamicRouteStack;
import com.sequenceiq.it.util.HostNameUtil;

public class ClouderaManagerDownscaleTest extends AbstractClouderaManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDownscaleTest.class);

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private static final BigDecimal HOSTS_DECOMMISSION_COMMAND_ID = new BigDecimal(300);

    private static final String CLOUDERA_MANAGER_KEY = "cm";

    private static final String CLUSTER_KEY = "cmcluster";

    private static final String LIST_HOSTS = API_ROOT + "/clusters/:clusterName/hosts";

    private static final String READ_HOSTS = API_ROOT + "/hosts";

    private static final String ADD_HOSTS = API_ROOT + "/clusters/:clusterName/hosts";

    private static final String DEPLOY_CLIENT_CONFIG = API_ROOT + "/clusters/:clusterName/commands/deployClientConfig";

    private static final String READ_PARCEL = API_ROOT + "/clusters/:clusterName/parcels/products/:product/versions/:version";

    private static final String APPLY_HOST_TEMPLATE = API_ROOT + "/clusters/:clusterName/hostTemplates/:hostTemplateName/commands/applyHostTemplate";

    private static final String READ_HOST = API_ROOT + "/hosts/:hostId";

    private static final String HOSTS_DECOMMISSION = API_ROOT + "/cm/commands/hostsDecommission";

    private static final String CLUSTERS_SERVICES = API_ROOT + "/clusters/:clusterName/services";

    private static final String REMOVE_HOST = API_ROOT + "/clusters/:clusterName/hosts/:hostId";

    private static final String DELETE_HOST = API_ROOT + "/hosts/:hostId";

    private static final String DELETE_CREDENTIALS = API_ROOT + "/cm/commands/deleteCredentials";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    private Integer originalWorkerCount;

    private Integer desiredWorkerCount;

    private Integer desiredBackscaledWorkerCount;

    private Stack<String> parcelStageResponses;

    private Boolean firstListHostsRequest = Boolean.TRUE;

    @BeforeMethod
    public void setUp() {
        originalWorkerCount = 1;
        desiredWorkerCount = 15;
        desiredBackscaledWorkerCount = 6;

        parcelStageResponses = new Stack<>();
        prepareReadParcelStageStack();
    }

    private void prepareReadParcelStageStack() {
        parcelStageResponses.push("ACTIVATED");
        parcelStageResponses.push("ACTIVATING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("ACTIVATING");
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15 it downscale to 6",
            then = "stack is running")
    public void testDownscale(MockedTestContext testContext) {
        String blueprintName = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String clusterName = resourcePropertyProvider().getName();

        addClouderaManagerMocks(testContext);
        testContext
                .given(CLOUDERA_MANAGER_KEY, ClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(CLOUDERA_MANAGER_KEY)
                .given(StackTestDto.class).withCluster(CLUSTER_KEY)
                .withName(clusterName)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkerCount))
                .await(StackTestDto.class, STACK_AVAILABLE, 3000)
                .when(StackScalePostAction.valid().withDesiredCount(desiredBackscaledWorkerCount))
                .await(StackTestDto.class, STACK_AVAILABLE, 3000)
                .validate();
    }

    private void addClouderaManagerMocks(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();

        List<ApiHost> apiHostList = generateHosts(testContext.getModel().getInstanceMap().values());
        List<ApiHostRef> apiHostRefList = generateHostsRefs(testContext.getModel().getInstanceMap().values());

        ApiHostRefList originalVms = new ApiHostRefList().items(generateHostsRefs(testContext.getModel().getInstanceMap().values().stream()
                .filter(status -> status.getCloudVmInstanceStatus().getCloudInstance().getTemplate().getPrivateId() <= originalWorkerCount)
                .collect(Collectors.toList())));
        ApiHostRefList upscaledVms = new ApiHostRefList().items(generateHostsRefs(testContext.getModel().getInstanceMap().values()));

        dynamicRouteStack.get(LIST_HOSTS,
                (request, response) -> {
                    if (firstListHostsRequest) {
                        firstListHostsRequest = Boolean.FALSE;
                        return originalVms;
                    } else {
                        return upscaledVms;
                    }
                });
        dynamicRouteStack.get(READ_HOSTS, (request, response) -> new ApiHostList().items(apiHostList));
        dynamicRouteStack.post(ADD_HOSTS, (request, response) -> new ApiHostRefList().items(apiHostRefList));
        dynamicRouteStack.post(DEPLOY_CLIENT_CONFIG, (request, response) -> new ApiCommand().id(DEPLOY_CLIENT_CONFIG_COMMAND_ID));
        dynamicRouteStack.get(READ_PARCEL, (request, response) -> new ApiParcel().stage(parcelStageResponses.pop()));
        dynamicRouteStack.post(APPLY_HOST_TEMPLATE, (request, response) -> new ApiCommand().id(APPLY_HOST_TEMPLATE_COMMAND_ID));
        dynamicRouteStack.get(READ_HOST,
                (request, response) -> {
                    String hostId = request.params(":hostId");
                    return apiHostList.stream()
                            .filter(host -> hostId.equals(host.getHostId()))
                            .findFirst().get();
                });
        dynamicRouteStack.post(HOSTS_DECOMMISSION, (request, response) -> new ApiCommand().id(HOSTS_DECOMMISSION_COMMAND_ID));
        dynamicRouteStack.get(CLUSTERS_SERVICES, (request, response) -> new ApiServiceList().items(List.of()));
        dynamicRouteStack.delete(REMOVE_HOST, (request, response) -> new ApiHost());
        dynamicRouteStack.delete(DELETE_HOST, (request, response) -> new ApiHost());
        dynamicRouteStack.post(DELETE_CREDENTIALS, (request, response) -> new ApiCommand());
    }

    private List<ApiHostRef> generateHostsRefs(Collection<CloudVmMetaDataStatus> cloudVmMetadataStatusList) {
        return cloudVmMetadataStatusList.stream()
                .filter(status -> InstanceStatus.STARTED.equals(status.getCloudVmInstanceStatus().getStatus()))
                .map(CloudVmMetaDataStatus::getMetaData)
                .map(CloudInstanceMetaData::getPrivateIp)
                .map(HostNameUtil::generateHostNameByIp)
                .map(hostname -> new ApiHostRef().hostname(hostname).hostId(hostname))
                .collect(Collectors.toList());
    }

    private List<ApiHost> generateHosts(Collection<CloudVmMetaDataStatus> cloudVmMetadataStatusList) {
        return cloudVmMetadataStatusList.stream()
                .filter(status -> InstanceStatus.STARTED.equals(status.getCloudVmInstanceStatus().getStatus()))
                .map(CloudVmMetaDataStatus::getMetaData)
                .map(CloudInstanceMetaData::getPrivateIp)
                .map(HostNameUtil::generateHostNameByIp)
                .map(hostname -> new ApiHost().hostname(hostname).hostId(hostname))
                .collect(Collectors.toList());
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
