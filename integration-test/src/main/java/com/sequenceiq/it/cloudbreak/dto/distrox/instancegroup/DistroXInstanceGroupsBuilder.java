package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COORDINATOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.EXECUTOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.GATEWAY;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto.withHostGroup;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;

public class DistroXInstanceGroupsBuilder {

    private final TestContext testContext;

    private List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtoList;

    public DistroXInstanceGroupsBuilder(TestContext testContext) {
        this.testContext = testContext;
    }

    public DistroXInstanceGroupsBuilder defaultHostGroup() {
        distroXInstanceGroupTestDtoList = withHostGroup(testContext, MASTER, COMPUTE, WORKER, GATEWAY);
        return this;
    }

    public DistroXInstanceGroupsBuilder verticalScaleHostGroup() {
        distroXInstanceGroupTestDtoList = withHostGroup(testContext, MASTER, COORDINATOR, EXECUTOR);
        VolumeV4TestDto attachedVolumes = testContext.init(VolumeV4TestDto.class, testContext.getCloudPlatform());
        VolumeV1Request volumes = new VolumeV1Request();
        volumes.setCount(attachedVolumes.getRequest().getCount());
        volumes.setSize(attachedVolumes.getRequest().getSize());
        volumes.setType(attachedVolumes.getRequest().getType());
        getInstanceTemplates().forEach(template -> template.setAttachedVolumes(Set.of(volumes)));
        return this;
    }

    public DistroXInstanceGroupsBuilder withDiskEncryption() {
        CloudProvider cloudProvider = testContext.getCloudProvider();
        getInstanceTemplates().forEach(cloudProvider::setInstanceTemplateV1Parameters);
        return this;
    }

    public DistroXInstanceGroupsBuilder withEphemeralTemporaryStorage() {
        getInstanceTemplates().forEach(template -> template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES));
        return this;
    }

    public DistroXInstanceGroupsBuilder withInstanceType(String instanceType) {
        getInstanceTemplates().forEach(template -> template.setInstanceType(instanceType));
        return this;
    }

    public DistroXInstanceGroupsBuilder withStorageOptimizedInstancetype() {
        CloudProvider cloudProvider = testContext.getCloudProvider();
        getInstanceTemplates().forEach(template -> template.setInstanceType(cloudProvider.getStorageOptimizedInstanceType()));
        return this;
    }

    public DistroXInstanceGroupsBuilder withRecipes(Set<String> recipeNames) {
        getInstanceGroupRequests().forEach(instanceGroupRequest -> instanceGroupRequest.setRecipeNames(recipeNames));
        return this;
    }

    public List<DistroXInstanceGroupTestDto> build() {
        return distroXInstanceGroupTestDtoList;
    }

    private List<InstanceTemplateV1Request> getInstanceTemplates() {
        return distroXInstanceGroupTestDtoList.stream()
                .map(this::getInstanceTemplate)
                .collect(Collectors.toList());
    }

    private InstanceTemplateV1Request getInstanceTemplate(DistroXInstanceGroupTestDto distroXInstanceGroupTestDto) {
        return distroXInstanceGroupTestDto.getRequest().getTemplate();
    }

    private List<InstanceGroupV1Request> getInstanceGroupRequests() {
        return distroXInstanceGroupTestDtoList.stream()
                .map(this::getInstanceGroupRequest)
                .collect(Collectors.toList());
    }

    private InstanceGroupV1Request getInstanceGroupRequest(DistroXInstanceGroupTestDto distroXInstanceGroupTestDto) {
        return distroXInstanceGroupTestDto.getRequest();
    }
}
