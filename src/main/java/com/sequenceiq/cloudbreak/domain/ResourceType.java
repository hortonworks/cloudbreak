package com.sequenceiq.cloudbreak.domain;

public enum ResourceType {
    // AZURE
    VIRTUAL_MACHINE,
    CLOUD_SERVICE,
    BLOB,
    STORAGE,
    NETWORK,
    AFFINITY_GROUP,

    // AWS
    CLOUDFORMATION_STACK,

    // GCC
    DISK,
    ATTACHED_DISK,
    NETWORK_INTERFACE,
    FIREWALL,
    ROUTE
}
