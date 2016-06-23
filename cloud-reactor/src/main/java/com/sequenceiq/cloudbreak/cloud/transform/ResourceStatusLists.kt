package com.sequenceiq.cloudbreak.cloud.transform

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

object ResourceStatusLists {

    fun aggregate(cloudResourceStatuses: List<CloudResourceStatus>): CloudResourceStatus {

        var status: ResourceStatus? = null
        var statusReason = ""

        for (crs in cloudResourceStatuses) {
            val currentStatus = crs.status

            if (status == null) {
                status = currentStatus
            }

            when (currentStatus) {
                ResourceStatus.FAILED -> {
                    status = currentStatus
                    statusReason += crs.statusReason + "\n"
                }
                else -> if (currentStatus.isTransient) {
                    status = currentStatus
                }
            }
        }

        if (status == null) {
            status = ResourceStatus.FAILED
            statusReason += "Resources does not have any state"
        }


        return CloudResourceStatus(null, status, statusReason)
    }


}
