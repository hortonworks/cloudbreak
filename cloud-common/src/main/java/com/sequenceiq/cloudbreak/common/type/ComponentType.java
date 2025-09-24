package com.sequenceiq.cloudbreak.common.type;

public enum ComponentType {
    IMAGE, CONTAINER,
    HDP_REPO_DETAILS,
    CLOUDBREAK_DETAILS,
    STACK_TEMPLATE,
    SALT_STATE,
    HDF_REPO_DETAILS,
    CDH_PRODUCT_DETAILS,
    CM_REPO_DETAILS,
    TELEMETRY,
    CLUSTER_UPGRADE_PREPARED_IMAGES;

    public static ComponentType cdhProductDetails() {
        return CDH_PRODUCT_DETAILS;
    }
}
