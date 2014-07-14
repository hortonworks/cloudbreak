package com.sequenceiq.cloudbreak.domain;

public enum ResourceType {
    // AZURE
    VIRTUAL_MACHINE,
    CLOUD_SERVICE,
    STORAGE,
    NETWORK,
    AFFINITY_GROUP,

    // AWS
    CLOUDFORMATION_TEMPLATE_NAME
}
