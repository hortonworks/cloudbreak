package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.service.StatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.FileSystemConfigException

@Component
class StorageAccountStatusCheckerTask : StatusCheckerTask<StorageAccountCheckerContext> {

    override fun checkStatus(ctx: StorageAccountCheckerContext): Boolean {
        val azureClient = AzureRMClient(ctx.tenantId, ctx.appId, ctx.appPassword, ctx.subscriptionId)
        try {
            val storageStatus = azureClient.getStorageStatus(ctx.resourceGroupName, ctx.storageAccountName)
            if ("Succeeded" == storageStatus) {
                return true
            }
        } catch (e: Exception) {
            LOGGER.info("Exception occurred while getting status of storage account: ", e)
            return false
        }

        return false
    }

    override fun handleTimeout(storageAccountCheckerContext: StorageAccountCheckerContext) {
        throw FileSystemConfigException("Operation timed out while creating Azure storage account for the WASB filesystem.")
    }

    override fun successMessage(storageAccountCheckerContext: StorageAccountCheckerContext): String {
        return "Storage account for the WASB filesystem created successfully."
    }

    override fun exitPolling(storageAccountCheckerContext: StorageAccountCheckerContext): Boolean {
        return false
    }

    override fun handleException(e: Exception) {
        throw FileSystemConfigException("Exception occurred while creating Azure storage account for the WASB filesystem.", e)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StorageAccountStatusCheckerTask::class.java)
    }
}
