package com.sequenceiq.sdx.api.model;

public class SdxInstanceGroupRequest {

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
