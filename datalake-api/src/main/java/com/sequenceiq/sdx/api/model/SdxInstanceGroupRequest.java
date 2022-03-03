package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

public class SdxInstanceGroupRequest {

    @NotNull
    private String name;

    private String instanceType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }
}
