package com.sequenceiq.common.api.type;

public enum InstanceGroupName {
    MASTER("master"),
    WORKER("worker"),
    IDBROKER("idbroker"),
    GATEWAY("gateway"),
    AUXILIARY("auxiliary"),
    CORE("core"),
    SOLRHG("solrhg"),
    STORAGEHG("storagehg"),
    KAFKAHG("kafkahg"),
    RAZHG("razhg"),
    ATLASHG("atlashg"),
    HMSHG("hmshg");

    private final String name;

    InstanceGroupName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
