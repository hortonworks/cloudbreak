package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;
import static com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto.withHostGroup;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProvider;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class DistroXInstanceGroupsBuilder {

    private final TestContext testContext;

    private List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtoList;

    public DistroXInstanceGroupsBuilder(TestContext testContext) {
        this.testContext = testContext;
    }

    public DistroXInstanceGroupsBuilder defaultHostGroup() {
        distroXInstanceGroupTestDtoList = withHostGroup(testContext, MASTER, COMPUTE, WORKER);
        return this;
    }

    public DistroXInstanceGroupsBuilder withDiskEncryption() {
        CloudProvider cloudProvider = testContext.getCloudProvider();
        getInstanceTemplates().forEach(cloudProvider::setInstanceTemplateV1Parameters);
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
}
