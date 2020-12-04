package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

public enum TaskState {

    REQUESTED,
    STARTED,
    READY,
    FAILED,
    TIMEOUT
}
