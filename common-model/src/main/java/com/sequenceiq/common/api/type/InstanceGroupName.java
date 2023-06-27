package com.sequenceiq.common.api.type;

public enum InstanceGroupName {
    MASTER("master"),
    WORKER("worker"),
    IDBROKER("idbroker"),
    GATEWAY("gateway"),
    AUXILIARY("auxiliary"),
    CORE("core"),
    SOLR_SCALE_OUT("solrscaleout"),
    STORAGE_SCALE_OUT("storagescaleout"),
    KAFKA_SCALE_OUT("kafkascaleout"),
    RAZSCALEOUT("razscaleout"),
    ATLAS_SCALE_OUT("atlasscaleout"),
    HMS_SCALE_OUT("hmsscaleout");

    private final String name;

    InstanceGroupName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
