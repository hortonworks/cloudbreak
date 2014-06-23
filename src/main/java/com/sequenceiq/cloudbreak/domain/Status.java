package com.sequenceiq.cloudbreak.domain;

public enum Status {
    REQUESTED,
    CREATE_IN_PROGRESS,
    CREATE_COMPLETED,
    CREATE_FAILED,
    DELETE_IN_PROGRESS,
    DELETE_FAILED,
    DELETE_COMPLETED;
}
