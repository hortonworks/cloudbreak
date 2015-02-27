package com.sequenceiq.cloudbreak.controller.json;

import javax.validation.constraints.NotNull;

public class HostGroupJson {

    @NotNull
    private String name;
    @NotNull
    private String instanceGroupName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }
}
