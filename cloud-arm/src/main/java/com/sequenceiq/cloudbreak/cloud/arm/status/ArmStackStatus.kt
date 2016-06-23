package com.sequenceiq.cloudbreak.cloud.arm.status

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

object ArmStackStatus {

    fun mapResourceStatus(status: String): ResourceStatus {
        when (status) {
            "Accepted" -> return ResourceStatus.IN_PROGRESS
            "Ready" -> return ResourceStatus.UPDATED
            "Canceled" -> return ResourceStatus.FAILED
            "Failed" -> return ResourceStatus.FAILED
            "Deleted" -> return ResourceStatus.DELETED
            "Succeeded" -> return ResourceStatus.CREATED
            else -> return ResourceStatus.IN_PROGRESS
        }
    }
}