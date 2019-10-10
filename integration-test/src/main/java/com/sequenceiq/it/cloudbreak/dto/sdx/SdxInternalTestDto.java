package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.cloud.v4.mock.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;

@Prototype
public class SdxInternalTestDto extends AbstractSdxTestDto<SdxInternalClusterRequest, SdxClusterDetailResponse, SdxInternalTestDto>
        implements Purgable<SdxClusterResponse, SdxClient> {

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    private static final String DEFAULT_SDX_BLUEPRINT_NAME = "CDP 1.2 - SDX Light Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String DEFAULT_CM_PASSWORD = "Admin123";

    private static final String DEFAULT_CM_USER = "admin";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public SdxInternalTestDto(TestContext testContext) {
        super(new SdxInternalClusterRequest(), testContext);
    }

    @Override
    public SdxInternalTestDto valid() {
        withName(getResourcePropertyProvider().getName())
                .withStackRequest()
                .withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getName())
                .withClusterShape(getCloudProvider().getInternalClusterShape())
                .withTags(getCloudProvider().getTags());
        return getCloudProvider().sdxInternal(this);
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }

    public SdxInternalTestDto withStackRequest(StackV4Request stackV4Request) {
        getRequest().setStackV4Request(stackV4Request);
        return this;
    }

    public SdxInternalTestDto withDatabase(SdxDatabaseRequest sdxDatabaseRequest) {
        getRequest().setExternalDatabase(sdxDatabaseRequest);
        return this;
    }

    public SdxInternalTestDto withCloudStorage(SdxCloudStorageRequest cloudStorage) {
        getRequest().setCloudStorage(cloudStorage);
        return this;
    }

    public SdxInternalTestDto withEnvironmentKey(RunningParameter key) {
        EnvironmentTestDto env = getTestContext().get(key.getKey());
        if (env == null) {
            throw new IllegalArgumentException("Environment is not given with key: " + key.getKey());
        }
        if (env.getResponse() == null) {
            throw new IllegalArgumentException("Environment is not created, or GET response is not included");
        }
        String name = env.getResponse().getName();
        getRequest().setEnvironment(name);
        return this;
    }

    public SdxInternalTestDto withStackRequest() {
        StackTestDto stack = getTestContext().given(StackTestDto.class);
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);

        return withStackRequest(stack, cluster);
    }

    public SdxInternalTestDto withStackRequest(String stackKey, String clusterKey) {
        StackTestDto stack = getTestContext().given(stackKey, StackTestDto.class);
        ClusterTestDto cluster = getTestContext().given(clusterKey, ClusterTestDto.class);

        return withStackRequest(stack, cluster);
    }

    private SdxInternalTestDto withStackRequest(StackTestDto stack, ClusterTestDto cluster) {
        InstanceGroupTestDto instanceGroup = getTestContext().given(InstanceGroupTestDto.class);

        if (instanceGroup.getResponse() == null) {
            getTestContext()
                    .given("master", InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1)
                    .given("idbroker", InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1);
        }

        cluster.withName(cluster.getName())
                .withBlueprintName(DEFAULT_SDX_BLUEPRINT_NAME)
                .withValidateBlueprint(Boolean.FALSE);
        stack.withName(stack.getName())
                .withImageSettings(getCloudProvider().imageSettings(getTestContext().given(ImageSettingsTestDto.class)))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.sdxHostGroup(getTestContext()))
                .withInstanceGroups(MASTER.getName(), IDBROKER.getName())
                .withNetwork(getCloudProvider().network(getTestContext().given(NetworkV4TestDto.class)))
                .withStackAuthentication(getCloudProvider().stackAuthentication(given(StackAuthenticationTestDto.class)))
                .withGatewayPort(getCloudProvider().gatewayPort(stack))
                .withCluster(cluster);
        return withStackRequest(stack.getRequest());
    }

    public SdxInternalTestDto withTemplate(JSONObject templateJson) {
        StackTestDto stack = getTestContext().given(StackTestDto.class);
        ClusterTestDto cluster = getTestContext().given(ClusterTestDto.class);

        cluster.withName(cluster.getName())
                .withBlueprintName(DEFAULT_SDX_BLUEPRINT_NAME)
                .withValidateBlueprint(Boolean.FALSE)
                .withPassword(getClusterPassword(templateJson))
                .withUserName(getClusterUser(templateJson));
        stack.withName(getName())
                .withImageSettings(getImageCatalog(getTestContext().given(ImageSettingsTestDto.class), templateJson))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.sdxHostGroup(getTestContext()))
                .withInstanceGroups(MASTER.getName(), IDBROKER.getName())
                .withNetwork(getNetwork(getTestContext().given(NetworkV4TestDto.class), templateJson, getCloudProvider().getCloudPlatform()))
                .withStackAuthentication(getAuthentication(getTestContext().given(StackAuthenticationTestDto.class), templateJson))
                .withGatewayPort(getGatewayPort(stack, templateJson))
                .withCluster(cluster);
        return withStackRequest(stack.getRequest());
    }

    public SdxInternalTestDto withTags(Map<String, String> tags) {
        getRequest().setTags(tags);
        return this;
    }

    public SdxInternalTestDto withClusterShape(SdxClusterShape shape) {
        getRequest().setClusterShape(shape);
        return this;
    }

    public SdxInternalTestDto withEnvironment() {
        EnvironmentTestDto environment = getTestContext().given(EnvironmentTestDto.class);
        if (environment == null) {
            throw new IllegalArgumentException("Environment does not exist!");
        }
        return withEnvironmentName(environment.getName());
    }

    public SdxInternalTestDto withEnvironmentName(String environment) {
        getRequest().setEnvironment(environment);
        return this;
    }

    public SdxInternalTestDto withName(String name) {
        setName(name);
        return this;
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status) {
        return await(status, emptyRunningParameter());
    }

    public SdxInternalTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    @Override
    public CloudbreakTestDto refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Refresh resource with name: {}", getName());
        return when(sdxTestClient.describeInternal(), key("refresh-sdx-" + getName()));
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up sdx with name: {}", getName());
        when(sdxTestClient.deleteInternal(), key("delete-sdx-" + getName()));
        await(DELETED, new RunningParameter().withSkipOnFail(true));
    }

    @Override
    public List<SdxClusterResponse> getAll(SdxClient client) {
        SdxEndpoint sdxEndpoint = client.getSdxClient().sdxEndpoint();
        return sdxEndpoint.list(null).stream()
                .filter(s -> s.getName() != null)
                .map(s -> {
                    SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
                    sdxClusterResponse.setName(s.getName());
                    return sdxClusterResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(SdxClusterResponse entity) {
        return getName().startsWith(getResourcePropertyProvider().prefix());
    }

    @Override
    public void delete(TestContext testContext, SdxClusterResponse entity, SdxClient client) {
        String sdxName = entity.getName();
        try {
            client.getSdxClient().sdxEndpoint().delete(getName(), false);
            testContext.await(this, DELETED, key("wait-purge-sdx-" + getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", sdxName, ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public Class<SdxClient> client() {
        return SdxClient.class;
    }

    private NetworkV4TestDto getNetwork(NetworkV4TestDto network, JSONObject templateJson, CloudPlatform cloudPlatform) {
        return cloudPlatform == MOCK ? mockNetwork(network, templateJson) : awsNetwork(network, templateJson);
    }

    private String getSubnetCidr(JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("network").getString("subnetCIDR");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Subnet CIDR from template: {}", templateJson, e);
            String subnetCIDR = commonCloudProperties.getSubnetCidr();
            return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
        }
    }

    private MockNetworkV4Parameters getMockNetworkParameters(JSONObject templateJson) {
        MockCloudProvider provider = new MockCloudProvider();
        var parameters = new MockNetworkV4Parameters();

        String gateway = getInternetGatewayId(MOCK, templateJson);
        parameters.setInternetGatewayId(gateway == null ? provider.getInternetGatewayId() : gateway);

        String vpc = getVpcId(MOCK, templateJson);
        parameters.setVpcId(vpc == null ? provider.getVpcId() : vpc);

        String subnet = getSubnetId(MOCK, templateJson);
        parameters.setSubnetId(subnet == null ? provider.getSubnetId() : subnet);

        return parameters;
    }

    private NetworkV4TestDto mockNetwork(NetworkV4TestDto network, JSONObject templateJson) {
        return network.withSubnetCIDR(getSubnetCidr(templateJson))
                .withMock(getMockNetworkParameters(templateJson));
    }

    private AwsNetworkV4Parameters getAwsNetworkParameters(JSONObject templateJson) {
        AwsCloudProvider provider = new AwsCloudProvider();
        var parameters = new AwsNetworkV4Parameters();

        String vpc = getVpcId(AWS, templateJson);
        parameters.setVpcId(vpc == null ? provider.getVpcId() : vpc);

        String subnet = getSubnetId(AWS, templateJson);
        parameters.setSubnetId(subnet == null ? provider.getSubnetId() : subnet);

        return parameters;
    }

    private NetworkV4TestDto awsNetwork(NetworkV4TestDto network, JSONObject templateJson) {
        return network.withSubnetCIDR(getSubnetCidr(templateJson))
                .withAws(getAwsNetworkParameters(templateJson));
    }

    private ImageSettingsTestDto getImageCatalog(ImageSettingsTestDto image, JSONObject templateJson) {
        try {
            return image
                    .withImageCatalog(templateJson.getJSONObject("image").getString("catalog"))
                    .withImageId(templateJson.getJSONObject("image").getString("id"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Image Catalog from template: {}", templateJson, e);
            return getCloudProvider().imageSettings(getTestContext().get(ImageSettingsTestDto.class));
        }
    }

    private StackAuthenticationTestDto getAuthentication(StackAuthenticationTestDto authentication, JSONObject templateJson) {
        try {
            return authentication.withPublicKeyId(templateJson.getJSONObject("authentication").getString("publicKeyId"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Authentication from template: {}", templateJson, e);
            return getCloudProvider().stackAuthentication(getTestContext().get(StackAuthenticationTestDto.class));
        }
    }

    private String getVpcId(CloudPlatform cloudPlatform, JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("network").getJSONObject(cloudPlatform.name().toLowerCase()).getString("vpcId");
        } catch (JSONException e) {
            LOGGER.error("Cannot get VPC ID from template: {}", templateJson, e);
            return null;
        }
    }

    private String getInternetGatewayId(CloudPlatform cloudPlatform, JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("network").getJSONObject(cloudPlatform.name().toLowerCase()).getString("internetGatewayId");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Internet Gateway ID from template: {}", templateJson, e);
            return null;
        }
    }

    private String getSubnetId(CloudPlatform cloudPlatform, JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("network").getJSONObject(cloudPlatform.name().toLowerCase()).getString("subnetId");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Subnet ID from template: {}", templateJson, e);
            return null;
        }
    }

    private Integer getGatewayPort(StackTestDtoBase stack, JSONObject templateJson) {
        try {
            return Integer.parseInt(templateJson.getString("gatewayPort"));
        } catch (JSONException e) {
            LOGGER.error("Cannot get Gateway Port from template: {}", templateJson, e);
            MockCloudProvider mock = new MockCloudProvider();
            return mock.gatewayPort(stack);
        }
    }

    private String getClusterPassword(JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("cluster").getString("password");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Cluster Password from template: {}", templateJson, e);
            String password = commonCloudProperties.getClouderaManager().getDefaultPassword();
            return password == null ? DEFAULT_CM_PASSWORD : password;
        }
    }

    private String getClusterUser(JSONObject templateJson) {
        try {
            return templateJson.getJSONObject("cluster").getString("userName");
        } catch (JSONException e) {
            LOGGER.error("Cannot get Cluster User from template: {}", templateJson, e);
            String user = commonCloudProperties.getClouderaManager().getDefaultUser();
            return user == null ? DEFAULT_CM_USER : user;
        }
    }

    public SdxRepairRequest getSdxRepairRequest() {
        SdxRepairTestDto repair = getCloudProvider().sdxRepair(given(SdxRepairTestDto.class));
        if (repair == null) {
            throw new IllegalArgumentException("SDX Repair does not exist!");
        }
        return repair.getRequest();
    }
}
