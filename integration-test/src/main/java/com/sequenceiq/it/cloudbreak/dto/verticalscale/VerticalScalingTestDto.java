package com.sequenceiq.it.cloudbreak.dto.verticalscale;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

@Prototype
public class VerticalScalingTestDto extends AbstractCloudbreakTestDto<VerticalScaleRequest, Void, VerticalScalingTestDto> {

    public VerticalScalingTestDto(TestContext testContext) {
        super(new VerticalScaleRequest(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return this;
    }

    public String getGroupName() {
        return getRequest().getGroup();
    }

    public String getInstanceType() {
        return getRequest().getTemplate().getInstanceType();
    }

    public VerticalScalingTestDto withGroup(String group) {
        getRequest().setGroup(group);
        return this;
    }

    public VerticalScalingTestDto withInstanceType(String instanceType) {
        InstanceTemplateRequest instanceTemplateRequest = new InstanceTemplateRequest();
        instanceTemplateRequest.setInstanceType(instanceType);
        getRequest().setTemplate(instanceTemplateRequest);
        return this;
    }

    public VerticalScalingTestDto withFreeipaVerticalScale() {
        return getCloudProvider().freeIpaVerticalScalingTestDto(this);
    }

    public VerticalScalingTestDto withDistroXVerticalScale() {
        return getCloudProvider().distroXVerticalScalingTestDto(this);
    }

    public VerticalScalingTestDto withSdxVerticalScale() {
        return getCloudProvider().datalakeVerticalScalingTestDto(this);
    }
}
