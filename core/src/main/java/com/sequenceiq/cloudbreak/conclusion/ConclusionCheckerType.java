package com.sequenceiq.cloudbreak.conclusion;

public enum ConclusionCheckerType {

    DEFAULT,
    CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP,
    CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP,
    STACK_PROVISION;

}
