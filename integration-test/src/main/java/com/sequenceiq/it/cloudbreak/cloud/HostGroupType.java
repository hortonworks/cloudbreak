package com.sequenceiq.it.cloudbreak.cloud;

import com.sequenceiq.common.api.type.InstanceGroupType;

public enum HostGroupType {
    MASTER("master", InstanceGroupType.GATEWAY, 1),
    MASTER_ENT("master", InstanceGroupType.CORE, 2),
    MASTER_STREAMS("master", InstanceGroupType.CORE, 1),
    WORKER("worker", InstanceGroupType.CORE, 3),
    IDBROKER("idbroker", InstanceGroupType.CORE, 1),
    IDBROKER_ENT("idbroker", InstanceGroupType.CORE, 2),
    GATEWAY("gateway", InstanceGroupType.CORE, 0),
    GATEWAY_ENT("gateway", InstanceGroupType.GATEWAY, 2),
    COMPUTE("compute", InstanceGroupType.CORE, 1),
    COORDINATOR("coordinator", InstanceGroupType.CORE, 1),
    EXECUTOR("executor", InstanceGroupType.CORE, 1),
    SERVICES("Services", InstanceGroupType.GATEWAY, 1),
    MESSAGING("Messaging", InstanceGroupType.CORE, 1),
    NIFI("NiFi", InstanceGroupType.CORE, 1),
    ZOOKEEPER("ZooKeeper", InstanceGroupType.CORE, 1),
    AUXILIARY("auxiliary", InstanceGroupType.CORE, 1),
    CORE("core", InstanceGroupType.CORE, 3),
    SOLR_SCALE_OUT("solr_scale_out", InstanceGroupType.CORE, 0),
    STORAGE_SCALE_OUT("storage_scale_out", InstanceGroupType.CORE, 0),
    KAFKA_SCALE_OUT("kafka_scale_out", InstanceGroupType.CORE, 0),
    RAZ_SCALE_OUT("raz_scale_out", InstanceGroupType.CORE, 0),
    ATLAS_SCALE_OUT("atlas_scale_out", InstanceGroupType.CORE, 0),
    HMS_SCALE_OUT("hms_scale_out", InstanceGroupType.CORE, 0),
    MANAGER("manager", InstanceGroupType.GATEWAY, 2),
    CORE_ZOOKEEPER("core_zookeeper", InstanceGroupType.CORE, 3),
    CORE_BROKER("core_broker", InstanceGroupType.CORE, 3),
    BROKER("broker", InstanceGroupType.CORE, 0),
    SRM("srm", InstanceGroupType.CORE, 0),
    CONNECT("connect", InstanceGroupType.CORE, 0),
    KRAFT("kraft", InstanceGroupType.CORE, 0);

    private final String name;

    private final InstanceGroupType instanceGroupType;

    private final int instanceCount;

    HostGroupType(String name, InstanceGroupType instanceGroupType, int instanceCount) {
        this.name = name;
        this.instanceGroupType = instanceGroupType;
        this.instanceCount = instanceCount;
    }

    public static HostGroupType getByName(String name) {
        for (HostGroupType value : HostGroupType.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public int determineInstanceCount() {
        return this.instanceCount;
    }
}
