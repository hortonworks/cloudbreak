package com.sequenceiq.it.cloudbreak.dto.stack;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;

@Prototype
public class StackTemplateTestDto extends StackTestDtoBase<StackTemplateTestDto> {

    public StackTemplateTestDto(TestContext testContext) {
        super(testContext);
    }

    public StackTemplateTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withCluster(getTestContext().init(ClusterTestDto.class))
                .withAuthentication(getTestContext().init(StackTestDto.class).getRequest().getAuthentication());
    }

    public StackTemplateTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public StackTemplateTestDto withCluster(ClusterTestDto cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public StackTemplateTestDto withImageSettings() {
        getRequest().setImage(getTestContext().get(ImageSettingsTestDto.class).getRequest());
        return this;
    }

    public StackTemplateTestDto withNewInstanceGroup() {
        withInstanceGroup(getTestContext().init(InstanceGroupTestDto.class).getRequest());
        return this;
    }

    private StackTemplateTestDto withInstanceGroup(InstanceGroupV4Request request) {
        getRequest().setInstanceGroups(List.of(request));
        return this;
    }

    private StackTemplateTestDto withAuthentication(StackAuthenticationV4Request authentication) {
        getRequest().setAuthentication(authentication);
        return this;
    }

}
