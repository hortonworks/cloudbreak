package com.sequenceiq.it.cloudbreak.newway.testcase.mock;


import static com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification.verify;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.ClouderaManagerMock.API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class ClouderaManagerUpscaleTest extends AbstractClouderaManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpscaleTest.class);

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private static final String CLOUDERA_MANAGER_KEY = "cm";

    private static final String CLUSTER_KEY = "cmcluster";

    private static final String LIST_HOSTS = API_ROOT + "/clusters/:clusterName/hosts";

    private static final String READ_HOSTS = API_ROOT + "/hosts";

    private static final String ADD_HOSTS = API_ROOT + "/clusters/:clusterName/hosts";

    private static final String DEPLOY_CLIENT_CONFIG = API_ROOT + "/clusters/:clusterName/commands/deployClientConfig";

    private static final String READ_PARCEL = API_ROOT + "/clusters/:clusterName/parcels/products/:product/versions/:version";

    private static final String APPLY_HOST_TEMPLATE = API_ROOT + "/clusters/:clusterName/hostTemplates/:hostTemplateName/commands/applyHostTemplate";

    private static final String READ_COMMAND = API_ROOT + "/commands/:commandId";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    private Integer originalWorkerCount;

    private Integer desiredWorkerCount;

    private Stack<String> parcelStageResponses;

    @BeforeMethod
    public void setUp() {
        originalWorkerCount = 1;
        desiredWorkerCount = 15;

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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15",
            then = "stack is running")
    public void testUpscale(MockedTestContext testContext) {
        String blueprintName = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String clusterName = resourcePropertyProvider().getName();

        Integer addedNodes = desiredWorkerCount - originalWorkerCount;

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
                .await(StackTestDto.class, STACK_AVAILABLE)
                .then(verify(POST, MOCK_ROOT + "/cloud_instance_statuses").exactTimes(1))
                .then(verify(POST, MOCK_ROOT + "/cloud_metadata_statuses")
                        .bodyContains("CREATE_REQUESTED", addedNodes).exactTimes(1))
                .then(verify(GET, SALT_BOOT_ROOT + "/health").atLeast(1))
                .then(verify(POST, SALT_BOOT_ROOT + "/salt/action/distribute").atLeast(1))
                .then(verify(POST, SALT_API_ROOT + "/run").bodyContains("fun=network.ipaddrs").atLeast(1))
                .then(verify(POST, SALT_API_ROOT + "/run").bodyContains("fun=saltutil.sync_all").atLeast(1))
                .then(verify(POST, SALT_API_ROOT + "/run").bodyContains("fun=mine.update").atLeast(1))
                .then(verify(POST, SALT_API_ROOT + "/run").bodyContains("fun=state.highstate").atLeast(2))
                .then(verify(POST, SALT_API_ROOT + "/run").bodyContains("fun=grains.remove").exactTimes(4))
                .then(verify(GET,
                        new ClouderaManagerPathResolver(LIST_HOSTS)
                                .pathVariableMapping(":clusterName", clusterName)
                                .resolve())
                        .exactTimes(1))
                .then(verify(GET, READ_HOSTS).exactTimes(2))
                .then(verify(POST, new ClouderaManagerPathResolver(ADD_HOSTS)
                        .pathVariableMapping(":clusterName", clusterName)
                        .resolve())
                        .exactTimes(1))
                .then(verify(POST, new ClouderaManagerPathResolver(DEPLOY_CLIENT_CONFIG)
                        .pathVariableMapping(":clusterName", clusterName)
                        .resolve())
                        .exactTimes(1))
                .then(verify(GET, new ClouderaManagerPathResolver(READ_PARCEL)
                        .pathVariableMapping(":clusterName", clusterName)
                        .pathVariableMapping(":product", "CDH")
                        .pathVariableMapping(":version", "6.2.0-1.cdh6.2.0.p0.967373")
                        .resolve())
                        .exactTimes(5))
                .then(verify(POST, new ClouderaManagerPathResolver(APPLY_HOST_TEMPLATE)
                        .pathVariableMapping(":clusterName", clusterName)
                        .pathVariableMapping(":hostTemplateName", "worker")
                        .resolve())
                        .exactTimes(1))
                .then(verify(GET, new ClouderaManagerPathResolver(READ_COMMAND)
                        .pathVariableMapping(":commandId", APPLY_HOST_TEMPLATE_COMMAND_ID.toString())
                        .resolve())
                        .exactTimes(1))
                .validate();
    }

    private void addClouderaManagerMocks(MockedTestContext testContext) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();

        List<CloudVmMetaDataStatus> originalVms = testContext.getModel().getInstanceMap().values().stream()
                .filter(status -> status.getCloudVmInstanceStatus().getCloudInstance().getTemplate().getPrivateId() <= originalWorkerCount)
                .collect(Collectors.toList());
        dynamicRouteStack.get(LIST_HOSTS,
                (request, response) -> new ApiHostRefList().items(generateHostsRefs(originalVms)));
        dynamicRouteStack.get(READ_HOSTS,
                (request, response) -> new ApiHostList().items(generateHosts(testContext.getModel().getInstanceMap().values())));
        dynamicRouteStack.post(ADD_HOSTS,
                (request, response) -> new ApiHostRefList().items(generateHostsRefs(testContext.getModel().getInstanceMap().values())));
        dynamicRouteStack.post(DEPLOY_CLIENT_CONFIG,
                (request, response) -> new ApiCommand().id(DEPLOY_CLIENT_CONFIG_COMMAND_ID));
        dynamicRouteStack.get(READ_PARCEL,
                (request, response) -> new ApiParcel().stage(parcelStageResponses.pop()));
        dynamicRouteStack.post(APPLY_HOST_TEMPLATE,
                (request, response) -> new ApiCommand().id(APPLY_HOST_TEMPLATE_COMMAND_ID));
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

    private static class ClouderaManagerPathResolver {

        private String pathTemplate;

        private final Map<String, String> pathVariableMap;

        ClouderaManagerPathResolver(String pathTemplate) {
            this.pathTemplate = pathTemplate;
            pathVariableMap = new HashMap<>();
        }

        ClouderaManagerPathResolver pathVariableMapping(String variable, String value) {
            pathVariableMap.put(variable, value);
            return this;
        }

        String resolve() {
            pathVariableMap.entrySet()
                    .forEach(mapping -> pathTemplate = pathTemplate.replace(mapping.getKey(), mapping.getValue()));
            return pathTemplate;
        }
    }
}
