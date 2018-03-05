package com.sequenceiq.cloudbreak.blueprint.nifi;

public class HdfConfigs {

    private String nodeEntities;

    public HdfConfigs(String nodeEntities) {
        this.nodeEntities = nodeEntities;
    }

    public String getNodeEntities() {
        return nodeEntities;
    }
}
