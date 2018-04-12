package com.sequenceiq.cloudbreak.template.processor;

public class HostgroupEntry {

    private String hostGroup;

    private HostgroupEntry(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public static HostgroupEntry hostgroupEntry(String hostGroup) {
        return new HostgroupEntry(hostGroup);
    }
}
