package com.sequenceiq.cloudbreak.cloud.openstack.nativ

import java.util.ArrayList

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus
import com.sequenceiq.cloudbreak.cloud.template.AbstractInstanceConnector

@Service
class OpenStackNativeInstanceConnector : AbstractInstanceConnector() {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun check(ac: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val osClient = openStackClient!!.createOSClient(ac)
        for (vm in vms) {
            val server = osClient.compute().servers().get(vm.instanceId)
            if (server == null) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED))
            } else {
                statuses.add(CloudVmInstanceStatus(vm, NovaInstanceStatus.get(server)))
            }
        }
        return statuses
    }

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        val osClient = openStackClient!!.createOSClient(authenticatedContext)
        return osClient.compute().servers().getConsoleOutput(vm.instanceId, CONSOLE_OUTPUT_LINES)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpenStackNativeInstanceConnector::class.java)
        private val CONSOLE_OUTPUT_LINES = Integer.MAX_VALUE
    }
}
