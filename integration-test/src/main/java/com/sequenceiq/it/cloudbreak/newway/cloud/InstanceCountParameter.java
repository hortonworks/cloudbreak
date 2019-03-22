package com.sequenceiq.it.cloudbreak.newway.cloud;

public enum InstanceCountParameter {
    MASTER_INSTANCE_COUNT("masterInstanceCount"),
    WORKER_INSTANCE_COUNT("workerInstanceCount"),
    COMPUTE_INSTANCE_COUNT("computeInstanceCount"),
    SERVICE_INSTANCE_COUNT("serviceInstanceCount"),
    NIFI_INSTANCE_COUNT("nifiInstanceCount"),
    ZOOKEEPER_INSTANCE_COUNT("zookeeperInstanceCount");

    private final String name;

    InstanceCountParameter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
