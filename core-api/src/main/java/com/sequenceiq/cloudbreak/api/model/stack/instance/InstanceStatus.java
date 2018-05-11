package com.sequenceiq.cloudbreak.api.model.stack.instance;

public enum InstanceStatus {
    REQUESTED, CREATED, UNREGISTERED, REGISTERED, DECOMMISSIONED, TERMINATED, DELETED_ON_PROVIDER_SIDE, FAILED, STOPPED
}
