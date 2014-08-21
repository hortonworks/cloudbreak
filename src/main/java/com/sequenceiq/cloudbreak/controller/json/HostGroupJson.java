package com.sequenceiq.cloudbreak.controller.json;

public class HostGroupJson {

    private String name;
    private Integer nodeCount;

    public HostGroupJson() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }
}
