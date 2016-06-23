package com.sequenceiq.cloudbreak.cloud.openstack.status

import com.google.common.base.Strings
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

object HeatStackStatus {

    fun mapResourceStatus(status: String): ResourceStatus {
        if (Strings.isNullOrEmpty(status) || status.contains("FAILED")) {
            return ResourceStatus.FAILED
        }

        when (status) {
            "CREATE_COMPLETE" -> return ResourceStatus.CREATED
            "DELETE_COMPLETE" -> return ResourceStatus.DELETED
            "UPDATE_COMPLETE" -> return ResourceStatus.UPDATED
            else -> return ResourceStatus.IN_PROGRESS
        }
    }
}
