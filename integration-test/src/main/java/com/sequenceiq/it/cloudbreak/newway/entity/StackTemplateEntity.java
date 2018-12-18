package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.SecurityRulesEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.testcase.ClusterTemplateTest;

@Prototype
public class StackTemplateEntity extends AbstractCloudbreakEntity<StackV2Request, StackResponse, StackTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateEntity.class);

    public StackTemplateEntity(TestContext testContext) {
        super(new StackV2Request(), testContext);
    }

    public StackTemplateEntity valid() {
        String randomNameForMock = getNameCreator().getRandomNameForMock();
        return withInputs(emptyMap())
                .withGeneralSettings(getTestContext().init(GeneralSettingsEntity.class))
                .withPlacementSettings(getTestContext().init(PlacementSettingsEntity.class))
                .withInstanceGroupsEntity(InstanceGroupEntity.defaultHostGroup(getTestContext()))
                .withNetwork(getCloudProvider().newNetwork(getTestContext()))
                .withCredentialName(getTestContext().get(CredentialEntity.class).getName())
                .withStackAuthentication(getTestContext().init(StackAuthentication.class))
                .withGatewayPort(getTestContext().getSparkServer().getPort())
                .withCluster(getTestContext().init(ClusterEntity.class).withName(randomNameForMock));
    }

    public StackTemplateEntity withEveryProperties() {
        ImageCatalogEntity imgCat = getTestContext().get(ImageCatalogEntity.class);
        getTestContext().given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("placementSettings", PlacementSettingsEntity.class).withRegion(ClusterTemplateTest.EUROPE)
                .given("network", NetworkV2Entity.class).withSubnetCIDR("10.10.0.0/16").withParameters(Map.of("customParameter", "subnet-value"))
                .given("securityRulesWorker", SecurityRulesEntity.class).withPorts("55,66,77").withProtocol("ftp").withSubnet("10.0.0.0/32")
                .given("securityGroupMaster", SecurityGroupEntity.class).withSecurityGroupIds("scgId1", "scgId2")
                .given("securityGroupWorker", SecurityGroupEntity.class).withSecurityRules("securityRulesWorker")
                .given("master", InstanceGroupEntity.class).withHostGroup(MASTER).withRecipes("mock-test-recipe").withSecurityGroup("securityGroupMaster")
                .given("worker", InstanceGroupEntity.class).withHostGroup(WORKER).withSecurityGroup("securityGroupWorker")
                .given("compute", InstanceGroupEntity.class).withHostGroup(COMPUTE)
                .given("mpackDetails", ManagementPackDetailsEntity.class).withName("mock-test-mpack")
                .given("ambariStack", AmbariStackDetailsEntity.class).withMpacks("mpackDetails")
                .given("ambariRepo", AmbariRepoDetailsEntity.class)
                .given("gatewayTopology", GatewayTopologyEntity.class).withExposedServices("AMBARI").withTopologyName("proxy-name")
                .given("gateway", GatewayEntity.class).withTopologies("gatewayTopology")
                .given("ambari", AmbariEntity.class).withAmbariRepoDetails("ambariRepo").withAmbariStackDetails("ambariStack").withGateway("gateway")
                .given("cluster", ClusterEntity.class).withRdsConfigNames("mock-test-rds").withLdapConfigName("mock-test-ldap").withAmbari("ambari")
                .given("imageSettings", ImageSettingsEntity.class).withImageId("f6e778fc-7f17-4535-9021-515351df3691").withImageCatalog(imgCat.getName());

        return withGeneralSettings("generalSettings")
                .withPlacementSettings("placementSettings")
                .withNetwork("network")
                .withInstanceGroups("master", "worker", "compute")
                .withCluster("cluster")
                .withUserDefinedTags(Map.of("some-tag", "custom-tag"))
                .withInputs(Map.of("some-input", "custom-input"))
                .withImageSettings("imageSettings")
                .withParameters(Map.of("param1", "some-value"));
    }

    public StackTemplateEntity withPlacementSettings(String key) {
        PlacementSettingsEntity placementSettings = getTestContext().get(key);
        return withPlacementSettings(placementSettings);
    }

    public StackTemplateEntity withPlacementSettings(PlacementSettingsEntity placementSettings) {
        getRequest().setPlacement(placementSettings.getRequest());
        return this;
    }

    public StackTemplateEntity withGeneralSettings(GeneralSettingsEntity generalSettings) {
        getRequest().setGeneral(generalSettings.getRequest());
        return this;
    }

    public StackTemplateEntity withGeneralSettings(String key) {
        GeneralSettingsEntity generalSettings = getTestContext().get(key);
        return withGeneralSettings(generalSettings);
    }

    public StackTemplateEntity withName(String name) {
        getRequest().getGeneral().setName(name);
        setName(name);
        return this;
    }

    public StackTemplateEntity withCredentialName(String credentialName) {
        getRequest().getGeneral().setCredentialName(credentialName);
        return this;
    }

    public StackTemplateEntity withCluster(String key) {
        ClusterEntity clusterEntity = getTestContext().get(key);
        return withCluster(clusterEntity);
    }

    public StackTemplateEntity withCluster(ClusterEntity cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public StackTemplateEntity withClusterRequest(ClusterV2Request clusterRequest) {
        getRequest().setCluster(clusterRequest);
        return this;
    }

    public StackTemplateEntity withAvailabilityZone(String availabilityZone) {
        getRequest().getPlacement().setAvailabilityZone(availabilityZone);
        return this;
    }

    public StackTemplateEntity withClusterNameAsSubdomain(boolean b) {
        if (getRequest().getCustomDomain() == null) {
            getRequest().setCustomDomain(new CustomDomainSettings());
        }
        getRequest().getCustomDomain().setClusterNameAsSubdomain(b);
        return this;
    }

    public StackTemplateEntity withFlexId(Long flexId) {
        getRequest().setFlexId(flexId);
        return this;
    }

    public StackTemplateEntity withImageSettings(String key) {
        ImageSettingsEntity imageSettingsEntity = getTestContext().get(key);
        getRequest().setImageSettings(imageSettingsEntity.getRequest());
        return this;
    }

    public StackTemplateEntity withImageCatalog(String imageCatalog) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageCatalog(imageCatalog);
        return this;
    }

    public StackTemplateEntity withImageId(String imageId) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageId(imageId);
        return this;
    }

    public StackTemplateEntity withInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            getRequest().setInputs(Collections.emptyMap());
        } else {
            getRequest().setInputs(inputs);
        }
        return this;
    }

    public StackTemplateEntity withInstanceGroups(List<InstanceGroupV2Request> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups);
        return this;
    }

    public StackTemplateEntity withInstanceGroupsEntity(Collection<InstanceGroupEntity> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(InstanceGroupEntity::getRequest)
                .collect(Collectors.toList()));
        return this;
    }

    public StackTemplateEntity withDefaultInstanceGroups() {
        return withInstanceGroups(InstanceGroupEntity.class.getSimpleName());
    }

    public StackTemplateEntity replaceInstanceGroups(String... keys) {
        Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .forEach(ig -> {
                    for (int i = 0; i < getRequest().getInstanceGroups().size(); i++) {
                        InstanceGroupV2Request old = getRequest().getInstanceGroups().get(i);
                        if (old.getGroup().equals(ig.getGroup())) {
                            getRequest().getInstanceGroups().remove(i);
                            getRequest().getInstanceGroups().add(i, ig);
                        }
                    }
                });
        return this;
    }

    private InstanceGroupV2Request getInstanceGroupV2Request(String key) {
        InstanceGroupEntity instanceGroupEntity = getTestContext().get(key);
        if (instanceGroupEntity == null) {
            throw new IllegalStateException("Given key is not exists in the test context: " + key);
        }
        return instanceGroupEntity.getRequest();
    }

    public StackTemplateEntity withInstanceGroups(String... keys) {
        getRequest().setInstanceGroups(Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .collect(Collectors.toList()));
        return this;
    }

    public StackTemplateEntity withNetwork(String key) {
        NetworkV2Entity network = getTestContext().get(key);
        return withNetwork(network);
    }

    public StackTemplateEntity withNetwork(NetworkV2Entity network) {
        getRequest().setNetwork(network.getRequest());
        return this;
    }

    public StackTemplateEntity withNetwork(NetworkV2Request network) {
        getRequest().setNetwork(network);
        return this;
    }

    public StackTemplateEntity withParameters(Map<String, String> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public StackTemplateEntity withRegion(String region) {
        getRequest().getPlacement().setRegion(region);
        return this;
    }

    public StackTemplateEntity withStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication);
        return this;
    }

    public StackTemplateEntity withStackAuthentication(StackAuthentication stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication.getRequest());
        return this;
    }

    public StackTemplateEntity withUserDefinedTags(Map<String, String> tags) {
        if (getRequest().getTags() == null) {
            getRequest().setTags(new Tags());
        }
        getRequest().getTags().setUserDefinedTags(tags);
        return this;
    }

    public StackTemplateEntity withAmbariVersion(String version) {
        getRequest().setAmbariVersion(version);
        return this;
    }

    public StackTemplateEntity withGatewayPort(int port) {
        getRequest().setGatewayPort(port);
        return this;
    }

    public boolean hasCluster() {
        return getRequest().getCluster() != null;
    }

    public List<InstanceGroupResponse> getInstanceGroups() {
        return getResponse().getInstanceGroups();
    }

    public String getInstanceId(String hostGroupName) {
        Set<InstanceMetaDataJson> metadata = getInstanceMetaData(hostGroupName);
        return metadata
                .stream()
                .findFirst()
                .get()
                .getInstanceId();
    }

    public Set<InstanceMetaDataJson> getInstanceMetaData(String hostGroupName) {
        return getResponse().getInstanceGroups()
                .stream().filter(im -> im.getGroup().equals(hostGroupName))
                .findFirst()
                .get()
                .getMetadata();
    }

    @Override
    public String getName() {
        return getRequest().getGeneral().getName();
    }
}
