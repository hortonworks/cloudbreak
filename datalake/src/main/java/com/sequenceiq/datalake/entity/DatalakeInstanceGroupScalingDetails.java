package com.sequenceiq.datalake.entity;

public enum DatalakeInstanceGroupScalingDetails {

    MASTER("master", false, 2),
    WORKER("worker", false, 0),
    IDBROKER("idbroker", false, 2),
    GATEWAY("gateway", false, 2),
    AUXILIARY("auxiliary", false, 1),
    CORE("core", true, 3),
    SOLR_SCALE_OUT("solr_scale_out", true, 0),
    STORAGE_SCALE_OUT("storage_scale_out", true, 0),
    KAFKA_SCALE_OUT("kafka_scale_out", true, 0),
    RAZ_SCALE_OUT("raz_scale_out", true, 0),
    ATLAS_SCALE_OUT("atlas_scale_out", true, 0),
    HMS_SCALE_OUT("hms_scale_out", true, 0);

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
