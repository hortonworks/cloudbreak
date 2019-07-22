package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.TemplateModelDescription;

import io.swagger.annotations.ApiModelProperty;

public abstract class InstanceTemplateBase {
    @ApiModelProperty(TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public String toString() {
        return "InstanceTemplateBase{"
                + "instanceType='" + instanceType + '\''
                + '}';
    }
}
