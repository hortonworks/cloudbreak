package com.sequenceiq.cloudbreak.cloud.openstack.status

import org.openstack4j.model.compute.Server

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

enum class NovaInstanceStatus private constructor(val status: String) {

    STARTED("ACTIVE"),
    STOPPED("SHUTOFF"),
    SUSPENDED("SUSPENDED"),
    PAUSED("PAUSED");


    companion object {

        operator fun get(server: Server): InstanceStatus {
            val status = server.status.toString()
            if (isStoppedInstanceStatus(status)) {
                return InstanceStatus.STOPPED
            } else if (status == STARTED.status) {
                return InstanceStatus.STARTED
            } else {
                return InstanceStatus.IN_PROGRESS
            }
        }

        private fun isStoppedInstanceStatus(status: String): Boolean {
            return status == STOPPED.status || status == SUSPENDED.status || status == PAUSED.status
        }
    }
}
