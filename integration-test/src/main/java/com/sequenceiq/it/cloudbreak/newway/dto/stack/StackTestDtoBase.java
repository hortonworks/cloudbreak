package com.sequenceiq.it.cloudbreak.newway.dto.stack;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AwsStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.GcpStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.MockStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.OpenStackStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.YarnStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.sharedservice.SharedServiceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.newway.SecurityRulesEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariRepositoryV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.EnvironmentSettingsV4TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.GatewayTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.GatewayTopologyTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ManagementPackDetailsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.NetworkV2TestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.SecurityGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.StackRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;

public abstract class StackTestDtoBase<T extends StackTestDtoBase<T>> extends AbstractCloudbreakTestDto<StackV4Request, StackV4Response, T> {

    public StackTestDtoBase(String newId) {
        super(newId);
        StackV4Request r = new StackV4Request();
        setRequest(r);
    }

    public StackTestDtoBase(TestContext testContext) {
        super(new StackV4Request(), testContext);
    }

    public StackTestDtoBase<T> valid() {
        String name = resourceProperyProvider().getName();
        withName(name)
                .withImageSettings(getCloudProvider().imageSettings(getTestContext().given(ImageSettingsTestDto.class)))
                .withPlacement(getTestContext().given(PlacementSettingsTestDto.class))
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(getTestContext()))
                .withNetwork(getTestContext().given(NetworkV2TestDto.class))
                .withStackAuthentication(getCloudProvider().stackAuthentication(given(StackAuthenticationTestDto.class)))
                .withGatewayPort(getCloudProvider().gatewayPort(this))
                .withCluster(getTestContext().given(ClusterTestDto.class).withName(name));
        return getCloudProvider().stack(this);
    }

    public StackTestDtoBase<T> withEveryProperties() {
        ImageCatalogTestDto imgCat = getTestContext().get(ImageCatalogTestDto.class);
        getTestContext()
                .given("network", NetworkV2TestDto.class).withSubnetCIDR("10.10.0.0/16")
                .given("securityRulesWorker", SecurityRulesEntity.class).withPorts("55", "66", "77").withProtocol("ftp").withSubnet("10.0.0.0/32")
                .given("securityGroupMaster", SecurityGroupTestDto.class).withSecurityGroupIds("scgId1", "scgId2")
                .given("securityGroupWorker", SecurityGroupTestDto.class).withSecurityRules("securityRulesWorker")
                .given("master", InstanceGroupTestDto.class).withHostGroup(MASTER).withRecipes("mock-test-recipe").withSecurityGroup("securityGroupMaster")
                .given("worker", InstanceGroupTestDto.class).withHostGroup(WORKER).withSecurityGroup("securityGroupWorker")
                .given("compute", InstanceGroupTestDto.class).withHostGroup(COMPUTE)
                .given("mpackDetails", ManagementPackDetailsTestDto.class).withName("mock-test-mpack")
                .given("ambariStack", StackRepositoryTestDto.class).withMpacks("mpackDetails")
                .given("ambariRepo", AmbariRepositoryV4TestDto.class)
                .given("gatewayTopology", GatewayTopologyTestDto.class).withExposedServices("AMBARI").withTopologyName("proxy-name")
                .given("gateway", GatewayTestDto.class).withTopologies("gatewayTopology")
                .given("ambari", AmbariTestDto.class).withAmbariRepoDetails("ambariRepo").withStackRepository("ambariStack")
                .given("cluster", ClusterTestDto.class).withRdsConfigNames("mock-test-rds").withLdapConfigName("mock-test-ldap").withAmbari("ambari")
                .withGateway("gateway")
                .given("imageSettings", ImageSettingsTestDto.class).withImageId("f6e778fc-7f17-4535-9021-515351df3691").withImageCatalog(imgCat.getName());

        return withNetwork("network")
                .withInstanceGroups("master", "worker", "compute")
                .withCluster("cluster")
                .withUserDefinedTags(Map.of("some-tag", "custom-tag"))
                .withInputs(Map.of("some-input", "custom-input"))
                .withImageSettings("imageSettings");
    }

    public StackTestDtoBase<T> withEnvironmentSettings() {
        return withEnvironmentSettings(EnvironmentSettingsV4TestDto.class.getSimpleName());
    }

    public StackTestDtoBase<T> withEnvironmentSettings(EnvironmentSettingsV4TestDto environment) {
        getRequest().setEnvironment(environment.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withCloudPlatform(CloudPlatform cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public StackTestDtoBase<T> withEnvironment(Class<EnvironmentTestDto> environmentKey) {
        return withEnvironmentKey(environmentKey.getSimpleName());
    }

    public StackTestDtoBase<T> withCatalog(Class<ImageCatalogTestDto> catalogKey) {
        ImageCatalogTestDto catalog = getTestContext().get(catalogKey);
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog is null with given key: " + catalogKey);
        }
        getRequest().getImage().setCatalog(catalog.getName());
        return this;
    }

    public StackTestDtoBase<T> withEnvironmentKey(String environmentKey) {
        EnvironmentTestDto env = getTestContext().get(environmentKey);
        if (env == null) {
            throw new IllegalArgumentException("Env is null with given key: " + environmentKey);
        }
        return withEnvironmentSettings(getTestContext().init(EnvironmentSettingsV4TestDto.class)
                .withName(env.getName()));
    }

    public StackTestDtoBase<T> withEnvironmentSettings(String environmentKey) {
        EnvironmentSettingsV4TestDto environment = getTestContext().get(environmentKey);
        getRequest().setEnvironment(environment.getRequest());
        return this;
    }

    /**
     * @deprecated this is forbidden in newway.testcase
     */
    @Deprecated
    public StackTestDtoBase<T> withEnvironmentSettings(EnvironmentSettingsV4Request environment) {
        getRequest().setEnvironment(environment);
        return this;
    }

    public StackTestDtoBase<T> withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public StackTestDtoBase<T> withCluster() {
        return withCluster(ClusterTestDto.class.getSimpleName());
    }

    public StackTestDtoBase<T> withCluster(String key) {
        ClusterTestDto clusterEntity = getTestContext().get(key);
        return withCluster(clusterEntity);
    }

    public StackTestDtoBase<T> withCluster(ClusterTestDto cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withClusterRequest(ClusterV4Request clusterRequest) {
        getRequest().setCluster(clusterRequest);
        return this;
    }

    public StackTestDtoBase<T> withImageSettings(String key) {
        ImageSettingsTestDto imageSettingsEntity = getTestContext().get(key);
        getRequest().setImage(imageSettingsEntity.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withImageSettings(ImageSettingsTestDto imageSettings) {
        getRequest().setImage(imageSettings.getRequest());
        ImageCatalogTestDto imageCatalogTestDto = getTestContext().get(ImageCatalogTestDto.class);
        if (imageCatalogTestDto != null) {
            getRequest().getImage().setCatalog(imageCatalogTestDto.getName());
        }
        return this;
    }

    public StackTestDtoBase<T> withInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            getRequest().setInputs(Collections.emptyMap());
        } else {
            getRequest().setInputs(inputs);
        }
        return this;
    }

    public StackTestDtoBase<T> withInstanceGroups(List<InstanceGroupV4Request> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups);
        return this;
    }

    public StackTestDtoBase<T> withInstanceGroupsEntity(Collection<InstanceGroupTestDto> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(InstanceGroupTestDto::getRequest)
                .collect(Collectors.toList()));
        return this;
    }

    public StackTestDtoBase<T> withDefaultInstanceGroups() {
        return withInstanceGroups(InstanceGroupTestDto.class.getSimpleName());
    }

    public StackTestDtoBase<T> replaceInstanceGroups(String... keys) {
        Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .forEach(ig -> {
                    for (int i = 0; i < getRequest().getInstanceGroups().size(); i++) {
                        InstanceGroupV4Request old = getRequest().getInstanceGroups().get(i);
                        if (old.getName().equals(ig.getName())) {
                            getRequest().getInstanceGroups().remove(i);
                            getRequest().getInstanceGroups().add(i, ig);
                        }
                    }
                });
        return this;
    }

    private InstanceGroupV4Request getInstanceGroupV2Request(String key) {
        InstanceGroupTestDto instanceGroupTestDto = getTestContext().get(key);
        if (instanceGroupTestDto == null) {
            throw new IllegalStateException("Given key is not exists in the test context: " + key);
        }
        return instanceGroupTestDto.getRequest();
    }

    public StackTestDtoBase<T> withInstanceGroups(String... keys) {
        getRequest().setInstanceGroups(Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .collect(Collectors.toList()));
        return this;
    }

    public StackTestDtoBase<T> withNetwork(String key) {
        NetworkV2TestDto network = getTestContext().get(key);
        return withNetwork(network);
    }

    public StackTestDtoBase<T> withNetwork(NetworkV2TestDto network) {
        getRequest().setNetwork(network.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withAzure(AzureStackV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public StackTestDtoBase<T> withAws(AwsStackV4Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public StackTestDtoBase<T> withGcp(GcpStackV4Parameters gcp) {
        getRequest().setGcp(gcp);
        return this;
    }

    public StackTestDtoBase<T> withOpenStack(OpenStackStackV4Parameters openStack) {
        getRequest().setOpenstack(openStack);
        return this;
    }

    public StackTestDtoBase<T> withMock(MockStackV4Parameters mock) {
        getRequest().setMock(mock);
        return this;
    }

    public StackTestDtoBase<T> withYarn(YarnStackV4Parameters yarn) {
        getRequest().setYarn(yarn);
        return this;
    }

    public StackTestDtoBase<T> withStackAuthentication(StackAuthenticationV4Request stackAuthentication) {
        getRequest().setAuthentication(stackAuthentication);
        return this;
    }

    public StackTestDtoBase<T> withStackAuthentication(StackAuthenticationTestDto stackAuthentication) {
        getRequest().setAuthentication(stackAuthentication.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withUserDefinedTags(Map<String, String> tags) {
        if (getRequest().getTags() == null) {
            getRequest().setTags(new TagsV4Request());
        }
        getRequest().getTags().setUserDefined(tags);
        return this;
    }

    public StackTestDtoBase<T> withGatewayPort(Integer port) {
        getRequest().setGatewayPort(port);
        return this;
    }

    public boolean hasCluster() {
        return getRequest().getCluster() != null;
    }

    public List<InstanceGroupV4Response> getInstanceGroups() {
        return getResponse().getInstanceGroups();
    }

    public String getInstanceId(String hostGroupName) {
        Set<InstanceMetaDataV4Response> metadata = getInstanceMetaData(hostGroupName);
        return metadata
                .stream()
                .findFirst()
                .get()
                .getInstanceId();
    }

    public Set<InstanceMetaDataV4Response> getInstanceMetaData(String hostGroupName) {
        return getResponse().getInstanceGroups()
                .stream().filter(im -> im.getName().equals(hostGroupName))
                .findFirst()
                .get()
                .getMetadata();
    }

    public StackTestDtoBase<T> withPlacement(String key) {
        PlacementSettingsTestDto placementSettings = getTestContext().get(key);
        return withPlacement(placementSettings);
    }

    public StackTestDtoBase<T> withPlacement(PlacementSettingsTestDto placementSettings) {
        getRequest().setPlacement(placementSettings.getRequest());
        return this;
    }

    public StackTestDtoBase<T> withSharedService(String datalakeClusterName) {
        SharedServiceV4Request sharedServiceRequest = new SharedServiceV4Request();
        sharedServiceRequest.setDatalakeName(datalakeClusterName);
        getRequest().setSharedService(sharedServiceRequest);
        return this;
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }
}
