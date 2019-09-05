package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;
import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.it.cloudbreak.newway.action.StackRefreshAction;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthentication;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.v3.StackV3Action;

public class StackEntity extends AbstractCloudbreakEntity<StackV2Request, StackResponse, StackEntity, StackViewResponse> {

    public static final String STACK = "STACK";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackEntity.class);

    StackEntity(String newId) {
        super(newId);
        StackV2Request r = new StackV2Request();
        r.setGeneral(new GeneralSettings());
        r.setPlacement(new PlacementSettings());
        setRequest(r);
    }

    public StackEntity() {
        this(STACK);
    }

    public StackEntity(StackV2Request request) {
        this();
        setRequest(request);
    }

    public StackEntity(TestContext testContext) {
        super(new StackV2Request(), testContext);
        getRequest().setGeneral(new GeneralSettings());
        getRequest().setPlacement(new PlacementSettings());
    }

    public StackEntity valid() {
        return withInputs(emptyMap())
                .withName(getNameCreator().getRandomNameForMock())
                .withRegion(getCloudProvider().region())
                .withAvailabilityZone(getCloudProvider().availabilityZone())
                .withInstanceGroupsEntity(InstanceGroupEntity.defaultHostGroup(getTestContext()))
                .withNetwork(getCloudProvider().newNetwork(getTestContext()))
                .withCredentialName(getTestContext().get(CredentialEntity.class).getName())
                .withStackAuthentication(getTestContext().init(StackAuthentication.class))
                .withGatewayPort(getTestContext().getSparkServer().getPort())
                .withCluster(getTestContext().init(ClusterEntity.class));
    }

    public StackEntity withName(String name) {
        getRequest().getGeneral().setName(name);
        setName(name);
        return this;
    }

    public StackEntity withCredentialName(String credentialName) {
        getRequest().getGeneral().setCredentialName(credentialName);
        return this;
    }

    public StackEntity withCluster(ClusterEntity cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public StackEntity withClusterRequest(ClusterV2Request clusterRequest) {
        getRequest().setCluster(clusterRequest);
        return this;
    }

    public StackEntity withAvailabilityZone(String availabilityZone) {
        getRequest().getPlacement().setAvailabilityZone(availabilityZone);
        return this;
    }

    public StackEntity withClusterNameAsSubdomain(boolean b) {
        if (getRequest().getCustomDomain() == null) {
            getRequest().setCustomDomain(new CustomDomainSettings());
        }
        getRequest().getCustomDomain().setClusterNameAsSubdomain(b);
        return this;
    }

    public StackEntity withFlexId(Long flexId) {
        getRequest().setFlexId(flexId);
        return this;
    }

    public StackEntity withImageCatalog(String imageCatalog) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageCatalog(imageCatalog);
        return this;
    }

    public StackEntity withImageId(String imageId) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageId(imageId);
        return this;
    }

    public StackEntity withInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            getRequest().setInputs(Collections.emptyMap());
        } else {
            getRequest().setInputs(inputs);
        }
        return this;
    }

    public StackEntity withInstanceGroups(List<InstanceGroupV2Request> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups);
        return this;
    }

    public StackEntity withInstanceGroupsEntity(Collection<InstanceGroupEntity> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(InstanceGroupEntity::getRequest)
                .collect(Collectors.toList()));
        return this;
    }

    public StackEntity withDefaultInstanceGroups() {
        return withInstanceGroups(InstanceGroupEntity.class.getSimpleName());
    }

    public StackEntity replaceInstanceGroups(String... keys) {
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

    public StackEntity withInstanceGroups(String... keys) {
        getRequest().setInstanceGroups(Stream.of(keys)
                .map(this::getInstanceGroupV2Request)
                .collect(Collectors.toList()));
        return this;
    }

    public StackEntity withNetwork(NetworkV2Entity network) {
        getRequest().setNetwork(network.getRequest());
        return this;
    }

    public StackEntity withNetwork(NetworkV2Request network) {
        getRequest().setNetwork(network);
        return this;
    }

    public StackEntity withParameters(Map<String, String> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public StackEntity withRegion(String region) {
        getRequest().getPlacement().setRegion(region);
        return this;
    }

    public StackEntity withStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication);
        return this;
    }

    public StackEntity withStackAuthentication(StackAuthentication stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication.getRequest());
        return this;
    }

    public StackEntity withUserDefinedTags(Map<String, String> tags) {
        if (getRequest().getTags() == null) {
            getRequest().setTags(new Tags());
        }
        getRequest().getTags().setUserDefinedTags(tags);
        return this;
    }

    public StackEntity withAmbariVersion(String version) {
        getRequest().setAmbariVersion(version);
        return this;
    }

    public StackEntity withGatewayPort(int port) {
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
        return Optional.ofNullable(metadata
                .stream()
                .findFirst()
                .orElse(new InstanceMetaDataJson())
                .getInstanceId()).orElse("");
    }

    public Set<InstanceMetaDataJson> getInstanceMetaData(String hostGroupName) {
        return Optional.ofNullable(getResponse().getInstanceGroups()
                .stream().filter(im -> im.getGroup().equals(hostGroupName))
                .findFirst()
                .orElse(new InstanceGroupResponse())
                .getMetadata()).orElse(Set.of());
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(StackV3Action::deleteV2, withoutLogError());
        await(AbstractIntegrationTest.STACK_DELETED);
    }

    @Override
    public List<StackResponse> getAll(CloudbreakClient client) {
        StackV3Endpoint stackV3Endpoint = client.getCloudbreakClient().stackV3Endpoint();
        return stackV3Endpoint.listByWorkspace(client.getWorkspaceId()).stream()
                .map(s -> {
                    StackResponse stackResponse = new StackResponse();
                    stackResponse.setName(s.getName());
                    return stackResponse;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean deletable(StackResponse entity) {
        return entity.getName().startsWith("mock-");
    }

    @Override
    public void delete(StackResponse entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().stackV3Endpoint().deleteInWorkspace(client.getWorkspaceId(), entity.getName(), true, false);
            wait(AbstractIntegrationTest.STACK_DELETED, key("wait-purge-stack-" + entity.getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), e.getMessage(), e);
        }
    }

    @Override
    public CloudbreakEntity refresh(TestContext context, CloudbreakClient cloudbreakClient) {
        return when(new StackRefreshAction(), key("refresh-stack-" + getName()));
    }

    @Override
    public CloudbreakEntity wait(Map<String, String> desiredStatuses, RunningParameter runningParameter) {
        return await(desiredStatuses, runningParameter);
    }
}
