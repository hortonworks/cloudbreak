package com.sequenceiq.it.cloudbreak.dto.clustertemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class DistroXTemplateTestDto extends DeletableTestDto<DistroXV1Request, ClusterTemplateV4Response,
        DistroXTemplateTestDto, ClusterTemplateV4Response> {

    public DistroXTemplateTestDto(TestContext testContext) {
        super(new DistroXV1Request(), testContext);
    }

    public DistroXTemplateTestDto() {
        super(DistroXTemplateTestDto.class.getSimpleName().toUpperCase());
    }

    public DistroXTemplateTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withEnvironmentName(getTestContext().get(EnvironmentTestDto.class).getName())
                .withCluster(getTestContext().init(ClusterTestDto.class).getRequest())
                .withInstanceGroups(getTestContext().init(InstanceGroupTestDto.class).getRequest());
    }

    private DistroXTemplateTestDto withInstanceGroups(InstanceGroupV4Request instanceGroupTestDto) {
        InstanceGroupV1Request instanceGroup = new InstanceGroupV1Request();
        InstanceTemplateV1Request template = new InstanceTemplateV1Request();
        RootVolumeV1Request rootVolume = new RootVolumeV1Request();
        rootVolume.setSize(instanceGroupTestDto.getTemplate().getRootVolume().getSize());
        template.setRootVolume(rootVolume);
        instanceGroup.setTemplate(template);
        instanceGroup.setName(instanceGroupTestDto.getName());
        getRequest().setInstanceGroups(Set.of(instanceGroup));
        return this;
    }

    public DistroXTemplateTestDto withCluster(String key) {
        withCluster(((ClusterTestDto) getTestContext().get(key)).getRequest());
        return this;
    }

    private DistroXTemplateTestDto withCluster(ClusterV4Request clusterV4Request) {
        DistroXClusterV1Request cluster = new DistroXClusterV1Request();
        cluster.setBlueprintName(clusterV4Request.getBlueprintName());
        ClouderaManagerV1Request cm = new ClouderaManagerV1Request();
        ClouderaManagerRepositoryV1Request repository = new ClouderaManagerRepositoryV1Request();
        cm.setRepository(repository);
        cluster.setCm(cm);
        cluster.setUserName(clusterV4Request.getUserName());
        cluster.setPassword(clusterV4Request.getPassword());
        getRequest().setCluster(cluster);
        return this;
    }

    public DistroXTemplateTestDto withBlueprintName(String value) {
        Optional.ofNullable(getRequest().getCluster())
                .ifPresentOrElse(r -> getRequest().getCluster().setBlueprintName(value), () -> {
                    throw new IllegalStateException("You can't set the blueprint name empty if there is no existing DistroXClusterV1Request on the " +
                            "DistroXTemplateTestDto!");
                });
        return this;
    }

    public DistroXTemplateTestDto withDefaultThreeInstanceGroups() {
        Collection<DistroXInstanceGroupTestDto> instanceGroups = DistroXInstanceGroupTestDto.defaultHostGroup(getTestContext());
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(DistroXInstanceGroupTestDto::getRequest)
                .collect(Collectors.toSet()));
        return this;
    }

    public DistroXTemplateTestDto withEnvironmentName(String name) {
        getRequest().setEnvironmentName(name);
        return this;
    }

    public DistroXTemplateTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public DistroXTemplateTestDto withImage() {
        DistroXImageTestDto imageTestDto = getTestContext().get(DistroXImageTestDto.class);
        getRequest().setImage(imageTestDto.getRequest());
        return this;
    }

    public DistroXTemplateTestDto withNetwork(String key) {
        DistroXNetworkTestDto networkTestDto = getTestContext().get(key);
        getRequest().setNetwork(networkTestDto.getRequest());
        return this;
    }

    public DistroXTemplateTestDto withMockedGatewayPort() {
        Integer gatewayPort = ((MockedTestContext) getTestContext()).getSparkServer().getPort();
        getRequest().setGatewayPort(gatewayPort);
        return this;
    }

    @Override
    protected String name(ClusterTemplateV4Response entity) {
        return null;
    }

    @Override
    public Collection<ClusterTemplateV4Response> getAll(CloudbreakClient client) {
        return List.of();
    }

    @Override
    public void delete(TestContext testContext, ClusterTemplateV4Response entity, CloudbreakClient client) {

    }
}
