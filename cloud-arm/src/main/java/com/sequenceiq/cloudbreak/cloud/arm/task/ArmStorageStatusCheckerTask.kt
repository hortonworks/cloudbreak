package com.sequenceiq.cloudbreak.cloud.arm.task

import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.cloud.arm.ArmClient
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask

import groovyx.net.http.HttpResponseException

@Component(ArmStorageStatusCheckerTask.NAME)
@Scope(value = "prototype")
class ArmStorageStatusCheckerTask(authenticatedContext: AuthenticatedContext, private val storageCheckerContext: StorageCheckerContext) : PollBooleanStateTask(authenticatedContext, true) {

    @Inject
    private val armClient: ArmClient? = null

    override fun call(): Boolean? {
        val client = armClient!!.getClient(storageCheckerContext.armCredentialView)
        var status = StorageStatus.OTHER
        try {
            val storageStatus = client.getStorageStatus(storageCheckerContext.groupName, storageCheckerContext.storageName)
            if (StorageStatus.SUCCEEDED.value == storageStatus) {
                status = StorageStatus.SUCCEEDED
            }
        } catch (e: HttpResponseException) {
            if (e.statusCode == NOT_FOUND) {
                status = StorageStatus.NOTFOUND
            } else {
                LOGGER.warn("HttpResponseException occured: {}", e.message)
            }
        } catch (ex: Exception) {
            LOGGER.warn("Error has happened while polling storage account: {}", ex.message)
        }

        if (storageCheckerContext.expectedStatus == status) {
            return true
        }
        return false
    }

    enum class StorageStatus private constructor(val value: String) {
        SUCCEEDED("Succeeded"), NOTFOUND("NotFound"), OTHER("Other")
    }

    companion object {
        val NAME = "armStorageStatusCheckerTask"

        private val LOGGER = LoggerFactory.getLogger(ArmStorageStatusCheckerTask::class.java)
    }

}
