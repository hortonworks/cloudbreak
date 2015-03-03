package com.sequenceiq.it.cloudbreak;

public class HostGroup {
    private String name;
    private String instanceGroupName;

    public HostGroup(String name, String instanceGroupName) {
        this.name = name;
        this.instanceGroupName = instanceGroupName;
    }

    public String getName() {
        return name;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }
}
