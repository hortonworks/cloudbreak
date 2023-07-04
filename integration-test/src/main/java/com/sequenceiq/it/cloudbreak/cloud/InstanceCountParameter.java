package com.sequenceiq.it.cloudbreak.cloud;

public enum InstanceCountParameter {
    MASTER_INSTANCE_COUNT("masterInstanceCount"),
    WORKER_INSTANCE_COUNT("workerInstanceCount"),
    IDBROKER_INSTANCE_COUNT("idbrokerInstanceCount"),
    GATEWAY_INSTANCE_COUNT("gatewayInstanceCount"),
    COMPUTE_INSTANCE_COUNT("computeInstanceCount"),
    SERVICE_INSTANCE_COUNT("serviceInstanceCount"),
    NIFI_INSTANCE_COUNT("nifiInstanceCount"),
    ZOOKEEPER_INSTANCE_COUNT("zookeeperInstanceCount"),
    AUXILIARY_INSTANCE_COUNT("auxiliaryInstanceCount"),
    CORE_INSTANCE_COUNT("coreInstanceCount"),
    SOLRHG_INSTANCE_COUNT("solrhgInstanceCount"),
    STORAGEHG_INSTANCE_COUNT("storagehgInstanceCount"),
    KAFKAHG_INSTANCE_COUNT("kafkahgInstanceCount"),
    RAZHG_INSTANCE_COUNT("razhgInstanceCount"),
    ATLASHG_INSTANCE_COUNT("atlashgInstanceCount"),
    HMSHG_INSTANCE_COUNT("hmshgInstanceCount");

    private final String name;

    InstanceCountParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
