package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.stackauthentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.SecurityRulesEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public abstract class StackV4EntityBase<T extends StackV4EntityBase<T>> extends AbstractCloudbreakEntity<StackV4Request, StackV4Response, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV4EntityBase.class);

    public StackV4EntityBase(String newId) {
        super(newId);
        StackV4Request r = new StackV4Request();
        setRequest(r);
    }

    public StackV4EntityBase(TestContext testContext) {
        super(new StackV4Request(), testContext);
    }

    public StackV4EntityBase<T> valid() {
        String randomNameForMock = getNameCreator().getRandomNameForMock();
        return withInputs(emptyMap())
                .withEnvironment(getTestContext().get(EnvironmentSettingsV4Entity.class))
                .withInstanceGroupsEntity(InstanceGroupEntity.defaultHostGroup(getTestContext()))
                .withNetwork(getCloudProvider().newNetwork(getTestContext()).getRequest())
                .withStackAuthentication(getTestContext().init(StackAuthenticationEntity.class))
                .withGatewayPort(getTestContext().getSparkServer().getPort())
                .withCluster(getTestContext().init(ClusterEntity.class).withName(randomNameForMock));
    }

    public StackV4EntityBase<T> withEveryProperties() {
        ImageCatalogEntity imgCat = getTestContext().get(ImageCatalogEntity.class);
        getTestContext()
                .given("network", NetworkV2Entity.class).withSubnetCIDR("10.10.0.0/16")
                .given("securityRulesWorker", SecurityRulesEntity.class).withPorts("55,66,77").withProtocol("ftp").withSubnet("10.0.0.0/32")
                .given("securityGroupMaster", SecurityGroupEntity.class).withSecurityGroupIds("scgId1", "scgId2")
                .given("securityGroupWorker", SecurityGroupEntity.class).withSecurityRules("securityRulesWorker")
                .given("master", InstanceGroupEntity.class).withHostGroup(MASTER).withRecipes("mock-test-recipe").withSecurityGroup("securityGroupMaster")
                .given("worker", InstanceGroupEntity.class).withHostGroup(WORKER).withSecurityGroup("securityGroupWorker")
                .given("compute", InstanceGroupEntity.class).withHostGroup(COMPUTE)
                .given("mpackDetails", ManagementPackDetailsEntity.class).withName("mock-test-mpack")
                .given("ambariStack", StackRepositoryEntity.class).withMpacks("mpackDetails")
                .given("ambariRepo", AmbariRepositoryV4Entity.class)
                .given("gatewayTopology", GatewayTopologyEntity.class).withExposedServices("AMBARI").withTopologyName("proxy-name")
                .given("gateway", GatewayEntity.class).withTopologies("gatewayTopology")
                .given("ambari", AmbariEntity.class).withAmbariRepoDetails("ambariRepo").withStackRepository("ambariStack")
                .given("cluster", ClusterEntity.class).withRdsConfigNames("mock-test-rds").withLdapConfigName("mock-test-ldap").withAmbari("ambari").withGateway("gateway")
                .given("imageSettings", ImageSettingsEntity.class).withImageId("f6e778fc-7f17-4535-9021-515351df3691").withImageCatalog(imgCat.getName());

        return withNetwork("network")
                .withInstanceGroups("master", "worker", "compute")
                .withCluster("cluster")
                .withUserDefinedTags(Map.of("some-tag", "custom-tag"))
                .withInputs(Map.of("some-input", "custom-input"))
                .withImageSettings("imageSettings");
    }

    public StackV4EntityBase<T> withEnvironment(EnvironmentSettingsV4Entity environment) {
        getRequest().setEnvironment(environment.getRequest());
        return this;
    }

    public StackV4EntityBase<T> withEnvironmentSettings(String environmentKey) {
        EnvironmentSettingsV4Entity environment = getTestContext().get(environmentKey);
        getRequest().setEnvironment(environment.getRequest());
        return this;
    }

    public StackV4EntityBase<T> withEnvironmentSettings(EnvironmentSettingsV4Request environment) {
        getRequest().setEnvironment(environment);
        return this;
    }

    public StackV4EntityBase<T> withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public StackV4EntityBase<T> withCluster(String key) {
        ClusterEntity clusterEntity = getTestContext().get(key);
        return withCluster(clusterEntity);
    }

    public StackV4EntityBase<T> withCluster(ClusterEntity cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public StackV4EntityBase<T> withClusterRequest(ClusterV4Request clusterRequest) {
        getRequest().setCluster(clusterRequest);
        return this;
    }

    public StackV4EntityBase<T> withFlexId(Long flexId) {
        getRequest().setFlexId(flexId);
        return this;
    }

    public StackV4EntityBase<T> withImageSettings(String key) {
        ImageSettingsEntity imageSettingsEntity = getTestContext().get(key);
        getRequest().setImage(imageSettingsEntity.getRequest());
        return this;
    }

    public StackV4EntityBase<T> withImageCatalog(String imageCatalog) {
        if (getRequest().getImage() == null) {
            getRequest().setImage(new ImageSettingsV4Request());
        }
        getRequest().getImage().setCatalog(imageCatalog);
        return this;
    }

    public StackV4EntityBase<T> withImageId(String imageId) {
        if (getRequest().getImage() == null) {
            getRequest().setImage(new ImageSettingsV4Request());
        }
        getRequest().getImage().setId(imageId);
        return this;
    }

    public StackV4EntityBase<T> withInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            getRequest().setInputs(emptyMap());
        } else {
            getRequest().setInputs(inputs);
        }
        return this;
    }

    public StackV4EntityBase<T> withInstanceGroups(List<InstanceGroupV4Request> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups);
        return this;
    }

    public StackV4EntityBase<T> withInstanceGroupsEntity(Collection<InstanceGroupEntity> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(InstanceGroupEntity::getRequest)
                .collect(Collectors.toList()));
        return this;
    }

    public StackV4EntityBase<T> withDefaultInstanceGroups() {
        return withInstanceGroups(InstanceGroupEntity.class.getSimpleName());
    }

    public StackV4EntityBase<T> replaceInstanceGroups(String... keys) {
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
        InstanceGroupEntity instanceGroupEntity = getTestContext().get(key);
        if (instanceGroupEntity == null) {
            throw new IllegalStateException("Given key is not exists in the test context: " + key);
        }
        return instanceGroupEntity.getRequest();
    }

    public StackV4EntityBase<T> withInstanceGroups(String... keys) {
        getRequest().setInstanceGroups(Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .collect(Collectors.toList()));
        return this;
    }

    public StackV4EntityBase<T> withNetwork(String key) {
        NetworkV2Entity network = getTestContext().get(key);
        return withNetwork(network.getRequest());
    }

    public StackV4EntityBase<T> withNetwork(NetworkV4Request network) {
        getRequest().setNetwork(network);
        return this;
    }

    public StackV4EntityBase<T> Network(NetworkV4Request network) {
        getRequest().setNetwork(network);
        return this;
    }

    public StackV4EntityBase<T> withAzure(AzureStackV4Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public StackV4EntityBase<T> withStackAuthentication(StackAuthenticationV4Request stackAuthentication) {
        getRequest().setAuthentication(stackAuthentication);
        return this;
    }

    public StackV4EntityBase<T> withStackAuthentication(StackAuthenticationEntity stackAuthentication) {
        getRequest().setAuthentication(stackAuthentication.getRequest());
        return this;
    }

    public StackV4EntityBase<T> withUserDefinedTags(Map<String, String> tags) {
        if (getRequest().getTags() == null) {
            getRequest().setTags(new TagsV4Request());
        }
        getRequest().getTags().setUserDefined(tags);
        return this;
    }

    public StackV4EntityBase<T> withAmbariVersion(String version) {
        getRequest().setAmbariVersion(version);
        return this;
    }

    public StackV4EntityBase<T> withGatewayPort(int port) {
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

    @Override
    public String getName() {
        return getRequest().getName();
    }
}
