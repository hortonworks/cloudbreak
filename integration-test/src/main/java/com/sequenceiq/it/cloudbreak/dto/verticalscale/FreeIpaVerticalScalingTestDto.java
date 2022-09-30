package com.sequenceiq.it.cloudbreak.dto.verticalscale;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.it.cloudbreak.Prototype;

@Prototype
public class FreeIpaVerticalScalingTestDto {

    private final String groupName;

    private final String instanceType;

    public FreeIpaVerticalScalingTestDto(String groupName, String instanceType) {
        this.groupName = groupName;
        this.instanceType = instanceType;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public VerticalScaleRequest getRequest() {
        VerticalScaleRequest verticalScaleRequest = new VerticalScaleRequest();
        verticalScaleRequest.setGroup(groupName);

        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceType);

        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }
}
