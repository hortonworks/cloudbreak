package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;


import static com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock.PROFILE_RETURN_HTTP_500;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiHostRefList;
import com.cloudera.api.swagger.model.ApiHostTemplateList;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.it.cloudbreak.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.util.HostNameUtil;
import com.sequenceiq.it.util.ResourceUtil;

public class CMUpscaleWithHttp500ResponsesTest extends AbstractClouderaManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMUpscaleWithHttp500ResponsesTest.class);

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private static final String CLOUDERA_MANAGER_KEY = "cm";

    private static final String CLUSTER_KEY = "cmcluster";

    private static final String LIST_HOSTS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hosts";

    private static final String READ_HOSTS = ClouderaManagerMock.API_ROOT + "/hosts";

    private static final String ADD_HOSTS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hosts";

    private static final String CLUSTERS_SERVICES = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/services";

    private static final String DEPLOY_CLIENT_CONFIG = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands/deployClientConfig";

    private static final String READ_PARCELS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/parcels";

    private static final String APPLY_HOST_TEMPLATE =
            ClouderaManagerMock.API_ROOT
                    + "/clusters/:clusterName/hostTemplates/:hostTemplateName/commands/applyHostTemplate";

    private static final String READ_COMMAND = ClouderaManagerMock.API_ROOT + "/commands/:commandId";

    private static final String LIST_CLUSTER_COMMANDS = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands";

    private static final String RESTART_MGMTSERVCIES_COMMAND = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/commands/restart";

    private static final String RESTART_CLUSTER_COMMAND = ClouderaManagerMock.API_ROOT + "/cm/service/commands/restart";

    private static final String READ_HOSTTEMPLATES = ClouderaManagerMock.API_ROOT + "/clusters/:clusterName/hostTemplates";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    private Integer originalWorkerCount;

    private Integer desiredWorkerCount;

    private Stack<String> parcelStageResponses;

    private Parcel parcel;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createCmBlueprint(testContext);
    }

    @BeforeMethod
    public void setUp() throws IOException {
        originalWorkerCount = 3;
        desiredWorkerCount = 15;

        parcelStageResponses = new Stack<>();
        prepareReadParcelStageStack();
        parcel = getParcel();
    }

    private void prepareReadParcelStageStack() {
        parcelStageResponses.push("ACTIVATED");
        parcelStageResponses.push("ACTIVATING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("DISTRIBUTING");
        parcelStageResponses.push("ACTIVATING");
    }

    private Parcel getParcel() throws IOException {
        String cmBlueprint = ResourceUtil.readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cmTemplateJson;
        try {
            cmTemplateJson = mapper.readTree(cmBlueprint);
        } catch (IOException e) {
            LOGGER.error("cannot deserialize clouderamanager.bp: " + cmBlueprint, e);
            throw new RuntimeException("cannot deserialize clouderamanager.bp", e);
        }
        JsonNode product = cmTemplateJson.get("products").get(0);
        String productName = product.get("product").asText();
        String version = product.get("version").asText();
        return new Parcel(productName, version);
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
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkerCount).withForced(Boolean.FALSE))
                .await(StackTestDto.class, STACK_AVAILABLE)
                .then(MockVerification.verify(POST, ITResponse.MOCK_ROOT + "/cloud_instance_statuses").exactTimes(1))
                .then(MockVerification.verify(POST, ITResponse.MOCK_ROOT + "/cloud_metadata_statuses")
                        .bodyContains("CREATE_REQUESTED", addedNodes).exactTimes(1))
                .then(MockVerification.verify(GET, ITResponse.SALT_BOOT_ROOT + "/health").atLeast(1))
                .then(MockVerification.verify(POST, ITResponse.SALT_BOOT_ROOT + "/salt/action/distribute").atLeast(1))
                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=network.ipaddrs").atLeast(1))
                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=saltutil.sync_all").atLeast(1))
                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=mine.update").atLeast(1))
                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=state.highstate").atLeast(2))
                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=grains.remove").exactTimes(6))
                .then(MockVerification.verify(GET,
                        new ClouderaManagerPathResolver(LIST_HOSTS)
                                .pathVariableMapping(":clusterName", clusterName)
                                .resolve())
                        .exactTimes(1))
                .then(MockVerification.verify(GET, READ_HOSTS).exactTimes(6))
                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(ADD_HOSTS)
                        .pathVariableMapping(":clusterName", clusterName)
                        .resolve())
                        .exactTimes(1))
                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(DEPLOY_CLIENT_CONFIG)
                        .pathVariableMapping(":clusterName", clusterName)
                        .resolve())
                        .exactTimes(1))
                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(APPLY_HOST_TEMPLATE)
                        .pathVariableMapping(":clusterName", clusterName)
                        .pathVariableMapping(":hostTemplateName", "worker")
                        .resolve())
                        .exactTimes(1))
                .then(MockVerification.verify(GET, new ClouderaManagerPathResolver(READ_COMMAND)
                        .pathVariableMapping(":commandId", APPLY_HOST_TEMPLATE_COMMAND_ID.toString())
                        .resolve())
                        .exactTimes(1))
                .validate();
    }

    @Override
    protected List<String> testProfiles() {
        return List.of(PROFILE_RETURN_HTTP_500);
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
        dynamicRouteStack.get(READ_PARCELS,
                (request, response) -> getMockedApiParcelList());
        dynamicRouteStack.post(APPLY_HOST_TEMPLATE,
                (request, response) -> new ApiCommand().id(APPLY_HOST_TEMPLATE_COMMAND_ID));
        dynamicRouteStack.post(RESTART_MGMTSERVCIES_COMMAND, (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        dynamicRouteStack.get(LIST_CLUSTER_COMMANDS, (request, response) -> new ApiCommandList().items(List.of()));
        dynamicRouteStack.post(RESTART_CLUSTER_COMMAND, (request, response) -> new ApiCommand().id(new BigDecimal(1)));
        dynamicRouteStack.get(READ_HOSTTEMPLATES, (request, response) -> new ApiHostTemplateList().items(List.of()));
        dynamicRouteStack.get(CLUSTERS_SERVICES, (request, response) -> new ApiServiceList().items(List.of()));
    }

    private ApiParcelList getMockedApiParcelList() {
        String stage = parcelStageResponses.empty() ? "ACTIVATED" : parcelStageResponses.pop();
        ApiParcel apiParcel = new ApiParcel()
                .product(parcel.getProduct())
                .version(parcel.getVersion())
                .stage(stage);
        return new ApiParcelList().items(List.of(apiParcel));
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
        List<ApiHost> apiHosts = new ArrayList<>();
        for (CloudVmMetaDataStatus vmStatus : cloudVmMetadataStatusList) {
            if (InstanceStatus.STARTED.equals(vmStatus.getCloudVmInstanceStatus().getStatus())) {
                String ip = vmStatus.getMetaData().getPrivateIp();
                String hostname = HostNameUtil.generateHostNameByIp(ip);
                apiHosts.add(new ApiHost().hostname(hostname).hostId(hostname).ipAddress(ip));
            }
        }
        return apiHosts;
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

    static class Parcel {
        private final String product;

        private final String version;

        Parcel(String product, String version) {
            this.product = product;
            this.version = version;
        }

        public String getProduct() {
            return product;
        }

        public String getVersion() {
            return version;
        }
    }
}
