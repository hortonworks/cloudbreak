package com.sequenceiq.cloudbreak.cloud.model

enum class InstanceStatus private constructor(val statusGroup: StatusGroup) {

    CREATED(StatusGroup.PERMANENT),
    STARTED(StatusGroup.PERMANENT),
    STOPPED(StatusGroup.PERMANENT),
    FAILED(StatusGroup.PERMANENT),
    TERMINATED(StatusGroup.PERMANENT),
    UNKNOWN(StatusGroup.PERMANENT),
    CREATE_REQUESTED(StatusGroup.PERMANENT),
    DELETE_REQUESTED(StatusGroup.PERMANENT),
    IN_PROGRESS(StatusGroup.TRANSIENT);

    val isPermanent: Boolean
        get() = StatusGroup.PERMANENT == statusGroup

    val isTransient: Boolean
        get() = StatusGroup.TRANSIENT == statusGroup
}
