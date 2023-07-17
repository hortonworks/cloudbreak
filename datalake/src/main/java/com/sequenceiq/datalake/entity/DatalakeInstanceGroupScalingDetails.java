package com.sequenceiq.datalake.entity;

public enum DatalakeInstanceGroupScalingDetails {

    MASTER("master", false, 2),
    WORKER("worker", false, 0),
    IDBROKER("idbroker", false, 2),
    GATEWAY("gateway", false, 2),
    AUXILIARY("auxiliary", false, 1),
    CORE("core", false, 3),
    SOLRHG("solrhg", true, 0),
    STORAGEHG("storagehg", true, 0),
    KAFKAHG("kafkahg", true, 0),
    RAZHG("razhg", true, 0),
    ATLASHG("atlashg", false, 0),
    HMSHG("hmshg", true, 0);

    private final String name;

    private final Boolean scalable;

    private final Integer minimumNodeCount;

    DatalakeInstanceGroupScalingDetails(String name, Boolean scalable, Integer minimumNodeCount) {
        this.name = name;
        this.scalable = scalable;
        this.minimumNodeCount = minimumNodeCount;
    }

    public String getName() {
        return name;
    }

    public Boolean isScalable() {
        return scalable;
    }

    public Integer getMinimumNodeCount() {
        return minimumNodeCount;
    }
}
