package com.sequenceiq.common.api.type;

public enum InstanceGroupName {
    MASTER("master"),
    WORKER("worker"),
    IDBROKER("idbroker"),
    GATEWAY("gateway"),
    AUXILIARY("auxiliary"),
    CORE("core"),
    SOLR_SCALE_OUT("solr_scale_out"),
    STORAGE_SCALE_OUT("storage_scale_out"),
    KAFKA_SCALE_OUT("kafka_scale_out"),
    RAZ_SCALE_OUT("raz_scale_out"),
    ATLAS_SCALE_OUT("atlas_scale_out"),
    HMS_SCALE_OUT("hms_scale_out");

    private final String name;

    InstanceGroupName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
