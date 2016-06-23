package com.sequenceiq.cloudbreak.cloud.openstack.heat

import java.util.ArrayList
import java.util.Optional

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Action
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.compute.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus

@Service
class OpenStackInstanceConnector : InstanceConnector {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        val osClient = openStackClient!!.createOSClient(authenticatedContext)
        return osClient.compute().servers().getConsoleOutput(vm.instanceId, CONSOLE_OUTPUT_LINES)
    }

    override fun start(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        return executeAction(ac, vms, Action.START)
    }

    override fun stop(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        return executeAction(ac, vms, Action.STOP)
    }

    override fun check(ac: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val osClient = openStackClient!!.createOSClient(ac)
        for (vm in vms) {
            val server = Optional.ofNullable<String>(vm.instanceId).map<Server>({ iid -> osClient.compute().servers().get(iid) })
            if (server.isPresent) {
                statuses.add(CloudVmInstanceStatus(vm, NovaInstanceStatus.get(server.get())))
            } else {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED))
            }
        }
        return statuses
    }

    private fun executeAction(ac: AuthenticatedContext, cloudInstances: List<CloudInstance>, action: Action): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val osClient = openStackClient!!.createOSClient(ac)
        for (cloudInstance in cloudInstances) {
            val actionResponse = osClient.compute().servers().action(cloudInstance.instanceId, action)
            if (actionResponse.isSuccess) {
                statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS))
            } else {
                statuses.add(CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, actionResponse.fault))
            }
        }
        return statuses
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackInstanceConnector::class.java)
        private val CONSOLE_OUTPUT_LINES = Integer.MAX_VALUE
    }
}
