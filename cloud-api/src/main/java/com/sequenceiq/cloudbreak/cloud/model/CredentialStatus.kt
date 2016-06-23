package com.sequenceiq.cloudbreak.cloud.model

enum class CredentialStatus private constructor(val statusGroup: StatusGroup) {

    CREATED(StatusGroup.PERMANENT),
    VERIFIED(StatusGroup.PERMANENT),
    DELETED(StatusGroup.PERMANENT),
    UPDATED(StatusGroup.PERMANENT),
    FAILED(StatusGroup.PERMANENT)


}
