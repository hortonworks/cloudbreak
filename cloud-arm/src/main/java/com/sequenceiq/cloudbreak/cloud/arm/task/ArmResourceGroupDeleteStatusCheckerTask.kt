package com.sequenceiq.cloudbreak.cloud.arm.task

import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient
import com.sequenceiq.cloudbreak.cloud.arm.context.ResourceGroupCheckerContext
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

import groovyx.net.http.HttpResponseException

@Component(ArmResourceGroupDeleteStatusCheckerTask.NAME)
@Scope(value = "prototype")
class ArmResourceGroupDeleteStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val armClient: ArmClient, private val resourceGroupDeleteCheckerContext: ResourceGroupCheckerContext) : PollBooleanStateTask(authenticatedContext, false) {

    override fun call(): Boolean? {
        val client = armClient.getClient(resourceGroupDeleteCheckerContext.armCredentialView)
        try {
            val resourceGroup = client.getResourceGroup(resourceGroupDeleteCheckerContext.groupName)
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
        val NAME = "armResourceGroupDeleteStatusCheckerTask"
    }
}
