package com.sequenceiq.it.cloudbreak.cloud;

import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.it.util.TestParameter;

public enum HostGroupType {
    MASTER("master", InstanceGroupType.GATEWAY, InstanceCountParameter.MASTER_INSTANCE_COUNT.getName()),
    MASTER_ENT("master", InstanceGroupType.CORE, InstanceCountParameter.MASTER_INSTANCE_COUNT.getName(), 2),
    WORKER("worker", InstanceGroupType.CORE, InstanceCountParameter.WORKER_INSTANCE_COUNT.getName(), 3),
    IDBROKER("idbroker", InstanceGroupType.CORE, InstanceCountParameter.IDBROKER_INSTANCE_COUNT.getName()),
    IDBROKER_ENT("idbroker", InstanceGroupType.CORE, InstanceCountParameter.IDBROKER_INSTANCE_COUNT.getName(), 2),
    GATEWAY("gateway", InstanceGroupType.CORE, InstanceCountParameter.GATEWAY_INSTANCE_COUNT.getName(), 0),
    GATEWAY_ENT("gateway", InstanceGroupType.GATEWAY, InstanceCountParameter.GATEWAY_INSTANCE_COUNT.getName(), 2),
    COMPUTE("compute", InstanceGroupType.CORE, InstanceCountParameter.COMPUTE_INSTANCE_COUNT.getName()),
    SERVICES("Services", InstanceGroupType.GATEWAY, InstanceCountParameter.SERVICE_INSTANCE_COUNT.getName()),
    MESSAGING("Messaging", InstanceGroupType.CORE, InstanceCountParameter.NIFI_INSTANCE_COUNT.getName()),
    NIFI("NiFi", InstanceGroupType.CORE, InstanceCountParameter.NIFI_INSTANCE_COUNT.getName()),
    ZOOKEEPER("ZooKeeper", InstanceGroupType.CORE, InstanceCountParameter.ZOOKEEPER_INSTANCE_COUNT.getName()),
    AUXILIARY("auxiliary", InstanceGroupType.CORE, InstanceCountParameter.AUXILIARY_INSTANCE_COUNT.getName(), 1),
    CORE("core", InstanceGroupType.CORE, InstanceCountParameter.CORE_INSTANCE_COUNT.getName(), 3),
    SOLR_SCALE_OUT("solr_scale_out", InstanceGroupType.CORE, InstanceCountParameter.SOLR_SCALE_OUT_INSTANCE_COUNT.getName()),
    STORAGE_SCALE_OUT("storage_scale_out", InstanceGroupType.CORE, InstanceCountParameter.STORAGE_SCALE_OUT_INSTANCE_COUNT.getName()),
    KAFKA_SCALE_OUT("kafka_scale_out", InstanceGroupType.CORE, InstanceCountParameter.KAFKA_SCALE_OUT_INSTANCE_COUNT.getName()),
    RAZ_SCALE_OUT("raz_scale_out", InstanceGroupType.CORE, InstanceCountParameter.RAZ_SCALE_OUT_INSTANCE_COUNT.getName(), 0),
    ATLAS_SCALE_OUT("atlas_scale_out", InstanceGroupType.CORE, InstanceCountParameter.ATLAS_SCALE_OUT_INSTANCE_COUNT.getName()),
    HMS_SCALE_OUT("hms_scale_out", InstanceGroupType.CORE, InstanceCountParameter.HMS_SCALE_OUT_INSTANCE_COUNT.getName());

    private final String name;

    private final String countParameterName;

    private final InstanceGroupType instanceGroupType;

    private final int defaultInstanceCount;

    HostGroupType(String name, InstanceGroupType instanceGroupType, String countParameterName) {
        this.name = name;
        this.instanceGroupType = instanceGroupType;
        this.countParameterName = countParameterName;
        defaultInstanceCount = 1;
    }

    HostGroupType(String name, InstanceGroupType instanceGroupType, String countParameterName, int defaultInstanceCount) {
        this.name = name;
        this.instanceGroupType = instanceGroupType;
        this.countParameterName = countParameterName;
        this.defaultInstanceCount = defaultInstanceCount;
    }

    public String getName() {
        return name;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public int determineInstanceCount(TestParameter testParameter) {
        String instanceCount = testParameter.get(countParameterName);
        int instanceCountInt;
        try {
            instanceCountInt = Integer.parseInt(instanceCount);
        } catch (NumberFormatException e) {
            instanceCountInt = this.defaultInstanceCount;
        }
        return instanceCountInt;
    }

    public static HostGroupType getByName(String name) {
        for (HostGroupType value : HostGroupType.values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return null;
    }
}
