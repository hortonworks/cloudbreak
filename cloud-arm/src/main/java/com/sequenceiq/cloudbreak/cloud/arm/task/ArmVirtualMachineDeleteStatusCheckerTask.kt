package com.sequenceiq.cloudbreak.cloud.arm.task

import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient
import com.sequenceiq.cloudbreak.cloud.arm.context.VirtualMachineCheckerContext
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

import groovyx.net.http.HttpResponseException

@Component(ArmVirtualMachineDeleteStatusCheckerTask.NAME)
@Scope(value = "prototype")
class ArmVirtualMachineDeleteStatusCheckerTask(ac: AuthenticatedContext, private val armClient: ArmClient, private val virtualMachineCheckerContext: VirtualMachineCheckerContext) : PollBooleanStateTask(ac, false) {

    override fun call(): Boolean? {
        val client = armClient.getClient(virtualMachineCheckerContext.armCredentialView)
        try {
            val virtualMachine = client.getVirtualMachine(virtualMachineCheckerContext.groupName,
                    virtualMachineCheckerContext.virtualMachine)
        } catch (e: HttpResponseException) {
            if (e.statusCode != NOT_FOUND) {
                throw CloudConnectorException(e.response.data.toString())
            } else {
                return true
            }
        } catch (ex: Exception) {
            return false
        }

        return false
    }

    companion object {
        val NAME = "armVirtualMachineDeleteStatusCheckerTask"
    }
}
