package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public enum HostGroupType {
    MASTER("master", InstanceGroupType.GATEWAY, InstanceCountParameter.MASTER_INSTANCE_COUNT.getName()),
    WORKER("worker", InstanceGroupType.CORE, InstanceCountParameter.WORKER_INSTANCE_COUNT.getName()),
    COMPUTE("compute", InstanceGroupType.CORE, InstanceCountParameter.COMPUTE_INSTANCE_COUNT.getName()),
    SERVICES("Services", InstanceGroupType.GATEWAY, InstanceCountParameter.SERVICE_INSTANCE_COUNT.getName()),
    MESSAGING("Messaging", InstanceGroupType.CORE, InstanceCountParameter.NIFI_INSTANCE_COUNT.getName()),
    NIFI("NiFi", InstanceGroupType.CORE, InstanceCountParameter.NIFI_INSTANCE_COUNT.getName()),
    ZOOKEEPER("ZooKeeper", InstanceGroupType.CORE, InstanceCountParameter.ZOOKEEPER_INSTANCE_COUNT.getName());

    private final String name;

    private final String countParameterName;

    private final InstanceGroupType instanceGroupType;

    HostGroupType(String name, InstanceGroupType instanceGroupType, String countParameterName) {
        this.name = name;
        this.instanceGroupType = instanceGroupType;
        this.countParameterName = countParameterName;
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
            instanceCountInt = 1;
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
