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
    GCC_DISK,
    GCC_ATTACHED_DISK,
    GCC_NETWORK,
    GCC_FIREWALL_IN,
    GCC_FIREWALL_OUT,
    GCC_ROUTE,
    GCC_INSTANCE,
}
