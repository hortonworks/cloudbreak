package com.sequenceiq.it.cloudbreak.dto.verticalscale;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.it.cloudbreak.Prototype;

@Prototype
public class DatalakeVerticalScalingTestDto {

    private final String groupName;

    private final String instanceType;

    public DatalakeVerticalScalingTestDto(String groupName, String instanceType) {
        this.groupName = groupName;
        this.instanceType = instanceType;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public StackVerticalScaleV4Request getRequest() {
        StackVerticalScaleV4Request verticalScaleRequest = new StackVerticalScaleV4Request();
        verticalScaleRequest.setGroup(groupName);

        InstanceTemplateV4Request instanceTemplateRequest = new InstanceTemplateV4Request();
        instanceTemplateRequest.setInstanceType(instanceType);

        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }
}
