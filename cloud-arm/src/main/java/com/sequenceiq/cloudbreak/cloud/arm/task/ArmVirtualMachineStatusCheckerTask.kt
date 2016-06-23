package com.sequenceiq.cloudbreak.cloud.arm.task

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

@Component(ArmVirtualMachineStatusCheckerTask.NAME)
@Scope(value = "prototype")
class ArmVirtualMachineStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val armClient: ArmClient, private val virtualMachineCheckerContext: VirtualMachineCheckerContext) : PollBooleanStateTask(authenticatedContext, true) {

    override fun call(): Boolean? {
        val client = armClient.getClient(virtualMachineCheckerContext.armCredentialView)
        try {
            val virtualMachine = client.getVirtualMachine(virtualMachineCheckerContext.groupName, virtualMachineCheckerContext.virtualMachine)
            val properties = virtualMachine["properties"] as Map<Any, Any>
            var statusCode = properties["provisioningState"].toString()
            statusCode = statusCode.replace("PowerState/", "")
            if (virtualMachineCheckerContext.status == statusCode) {
                return true
            }
        } catch (ex: Exception) {
            return false
        }

        return false
    }

    companion object {
        val NAME = "armVirtualMachineStatusCheckerTask"
    }
}
