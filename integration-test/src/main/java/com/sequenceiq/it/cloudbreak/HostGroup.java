package com.sequenceiq.it.cloudbreak;

public class HostGroup {

    private final String name;

    private final String instanceGroupName;

    private final Integer hostCount;

    public HostGroup(String name, String instanceGroupName, Integer hostCount) {
        this.name = name;
        this.instanceGroupName = instanceGroupName;
        this.hostCount = hostCount;
    }

    public String getName() {
        return name;
    }

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public Integer getHostCount() {
        return hostCount;
    }
}
