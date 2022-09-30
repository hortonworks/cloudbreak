package com.sequenceiq.it.cloudbreak.dto.verticalscale;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXVerticalScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;

@Prototype
public class DistroXVerticalScalingTestDto {

    private final String groupName;

    private final String instanceType;

    public DistroXVerticalScalingTestDto(String groupName, String instanceType) {
        this.groupName = groupName;
        this.instanceType = instanceType;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public DistroXVerticalScaleV1Request getRequest() {
        DistroXVerticalScaleV1Request verticalScaleRequest = new DistroXVerticalScaleV1Request();
        verticalScaleRequest.setGroup(groupName);

        InstanceTemplateV1Request instanceTemplateRequest = new InstanceTemplateV1Request();
        instanceTemplateRequest.setInstanceType(instanceType);

        verticalScaleRequest.setInstanceTemplateV1Request(instanceTemplateRequest);
        return verticalScaleRequest;
    }

}
