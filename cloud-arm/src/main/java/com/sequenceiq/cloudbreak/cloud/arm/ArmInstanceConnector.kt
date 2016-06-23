package com.sequenceiq.cloudbreak.cloud.arm

import java.util.ArrayList

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.arm.status.ArmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

import groovyx.net.http.HttpResponseException

@Service
class ArmInstanceConnector : InstanceConnector {

    @Inject
    private val armClient: ArmClient? = null

    @Inject
    private val armTemplateUtils: ArmUtils? = null

    override fun getConsoleOutput(authenticatedContext: AuthenticatedContext, vm: CloudInstance): String {
        throw CloudOperationNotSupportedException("Azure ARM doesn't provide access to the VM console output yet.")
    }

    override fun start(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val azureRMClient = armClient!!.getClient(ac.cloudCredential)
        val stackName = armTemplateUtils!!.getStackName(ac.cloudContext)
        val statuses = ArrayList<CloudVmInstanceStatus>()

        for (vm in vms) {
            try {
                azureRMClient.startVirtualMachine(stackName, vm.instanceId)
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS))
            } catch (e: HttpResponseException) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.response.data.toString()))
            } catch (e: Exception) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.message))
            }

        }
        return statuses
    }

    override fun stop(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val azureRMClient = armClient!!.getClient(ac.cloudCredential)
        val stackName = armTemplateUtils!!.getStackName(ac.cloudContext)
        val statuses = ArrayList<CloudVmInstanceStatus>()

        for (vm in vms) {
            try {
                azureRMClient.stopVirtualMachine(stackName, vm.instanceId)
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS))
            } catch (e: HttpResponseException) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.response.data.toString()))
            } catch (e: Exception) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.FAILED, e.message))
            }

        }
        return statuses
    }

    override fun check(ac: AuthenticatedContext, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val statuses = ArrayList<CloudVmInstanceStatus>()
        val azureRMClient = armClient!!.getClient(ac.cloudCredential)
        val stackName = armTemplateUtils!!.getStackName(ac.cloudContext)

        for (vm in vms) {
            try {
                val virtualMachine = azureRMClient.getVirtualMachineInstanceView(stackName, vm.instanceId)
                val vmStatuses = virtualMachine["statuses"] as List<Any>
                for (vmStatuse in vmStatuses) {
                    var statusCode = vmStatuse.get("code").toString()
                    if (statusCode.startsWith("PowerState")) {
                        statusCode = statusCode.replace("PowerState/", "")
                        statuses.add(CloudVmInstanceStatus(vm, ArmInstanceStatus.get(statusCode)))
                        break
                    }
                }
            } catch (e: Exception) {
                statuses.add(CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED))
            }

        }
        return statuses
    }
}
