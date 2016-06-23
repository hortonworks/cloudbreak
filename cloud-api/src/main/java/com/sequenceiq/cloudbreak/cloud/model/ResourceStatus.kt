package com.sequenceiq.cloudbreak.cloud.model

enum class ResourceStatus private constructor(val statusGroup: StatusGroup) {

    CREATED(StatusGroup.PERMANENT),
    DELETED(StatusGroup.PERMANENT),
    UPDATED(StatusGroup.PERMANENT),
    FAILED(StatusGroup.PERMANENT),
    IN_PROGRESS(StatusGroup.TRANSIENT);

    val isPermanent: Boolean
        get() = StatusGroup.PERMANENT == statusGroup

    val isTransient: Boolean
        get() = StatusGroup.TRANSIENT == statusGroup
}
