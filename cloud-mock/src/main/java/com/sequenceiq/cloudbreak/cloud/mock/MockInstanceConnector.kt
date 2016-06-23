package com.sequenceiq.cloudbreak.cloud.mock

import java.util.ArrayList

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

@Service
class MockInstanceConnector : InstanceConnector {

    @Throws(Exception::class)
    override fun start(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudVmInstanceStatuses = ArrayList<CloudVmInstanceStatus>()
        for (instance in vms) {
            val instanceStatus = CloudVmInstanceStatus(instance, InstanceStatus.CREATED)
            cloudVmInstanceStatuses.add(instanceStatus)
        }
        return cloudVmInstanceStatuses
    }

    @Throws(Exception::class)
    override fun stop(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudVmInstanceStatuses = ArrayList<CloudVmInstanceStatus>()
        for (instance in vms) {
            val instanceStatus = CloudVmInstanceStatus(instance, InstanceStatus.STOPPED)
            cloudVmInstanceStatuses.add(instanceStatus)
        }
        return cloudVmInstanceStatuses
    }

    override fun check(authenticatedContext: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudVmInstanceStatuses = ArrayList<CloudVmInstanceStatus>()
        for (instance in vms) {
            val instanceStatus = CloudVmInstanceStatus(instance, InstanceStatus.STARTED)
            cloudVmInstanceStatuses.add(instanceStatus)
        }
        return cloudVmInstanceStatuses
    }

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        return CB_FINGERPRINT
    }

    companion object {

        private val CB_FINGERPRINT = "ce:50:66:23:96:08:04:ea:01:62:9b:18:f9:ee:ac:aa (RSA)"
    }
}
