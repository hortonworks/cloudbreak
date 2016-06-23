package com.sequenceiq.cloudbreak.cloud.arm.status

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

enum class ArmInstanceStatus private constructor(val status: String) {

    STARTED("running"),
    STOPPED("stopped");


    companion object {

        operator fun get(status: String): InstanceStatus {
            when (status) {
                "stopped" -> return InstanceStatus.STOPPED
                "running" -> return InstanceStatus.STARTED
                else -> return InstanceStatus.IN_PROGRESS
            }
        }
    }
}
