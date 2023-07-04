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
    SOLRHG("solrhg", InstanceGroupType.CORE, InstanceCountParameter.SOLRHG_INSTANCE_COUNT.getName(), 0),
    STORAGEHG("storagehg", InstanceGroupType.CORE, InstanceCountParameter.STORAGEHG_INSTANCE_COUNT.getName(), 0),
    KAFKAHG("kafkahg", InstanceGroupType.CORE, InstanceCountParameter.KAFKAHG_INSTANCE_COUNT.getName(), 0),
    RAZHG("razhg", InstanceGroupType.CORE, InstanceCountParameter.RAZHG_INSTANCE_COUNT.getName(), 0),
    ATLASHG("atlashg", InstanceGroupType.CORE, InstanceCountParameter.ATLASHG_INSTANCE_COUNT.getName(), 0),
    HMSHG("hmshg", InstanceGroupType.CORE, InstanceCountParameter.HMSHG_INSTANCE_COUNT.getName(), 0);

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
