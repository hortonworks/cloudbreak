package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.io.Serializable;
import java.util.List;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.TemplateModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class InstanceTemplateBase implements Serializable {
    @Schema(description = TemplateModelDescription.INSTANCE_TYPE)
    private String instanceType;

    @Schema
    private List<String> fallbackInstanceTypes;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public List<String> getFallbackInstanceTypes() {
        return fallbackInstanceTypes;
    }

    public void setFallbackInstanceTypes(List<String> fallbackInstanceTypes) {
        this.fallbackInstanceTypes = fallbackInstanceTypes;
    }

    @Override
    public String toString() {
        return "InstanceTemplateBase{"
                + "instanceType='" + instanceType + '\''
                + ", fallbackInstanceTypes='" + fallbackInstanceTypes  + '\''
                + '}';
    }
}
