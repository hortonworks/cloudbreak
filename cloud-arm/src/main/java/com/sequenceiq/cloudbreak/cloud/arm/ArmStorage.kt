package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus.NOTFOUND
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmStorageStatusCheckerTask.StorageStatus.SUCCEEDED

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import javax.inject.Inject

import org.apache.commons.lang3.text.WordUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.common.base.Strings
import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption
import com.sequenceiq.cloudbreak.cloud.arm.context.StorageCheckerContext
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask

@Service
class ArmStorage {

    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null
    @Inject
    private val armPollTaskFactory: ArmPollTaskFactory? = null
    @Inject
    private val armUtils: ArmUtils? = null

    fun getArmAttachedStorageOption(parameters: Map<String, String>): ArmAttachedStorageOption {
        val attachedStorageOption = parameters["attachedStorageOption"]
        if (Strings.isNullOrEmpty(attachedStorageOption)) {
            return ArmAttachedStorageOption.SINGLE
        }
        return ArmAttachedStorageOption.valueOf(attachedStorageOption)
    }

    fun getImageStorageName(acv: ArmCredentialView, cloudContext: CloudContext, persistentStorageName: String,
                            armAttachedStorageOption: ArmAttachedStorageOption): String {
        val storageName: String
        if (isPersistentStorage(persistentStorageName)) {
            storageName = getPersistentStorageName(persistentStorageName, acv, cloudContext.location!!.region.value())
        } else {
            storageName = buildStorageName(armAttachedStorageOption, acv, null, cloudContext, ArmDiskType.LOCALLY_REDUNDANT)
        }
        return storageName
    }

    fun getAttachedDiskStorageName(armAttachedStorageOption: ArmAttachedStorageOption, acv: ArmCredentialView, vmId: Long?, cloudContext: CloudContext,
                                   storageType: ArmDiskType): String {
        return buildStorageName(armAttachedStorageOption, acv, vmId, cloudContext, storageType)
    }

    @Throws(Exception::class)
    fun createStorage(ac: AuthenticatedContext, client: AzureRMClient, osStorageName: String, storageType: ArmDiskType, storageGroup: String, region: String) {
        if (!storageAccountExist(client, osStorageName)) {
            client.createStorageAccount(storageGroup, osStorageName, region, storageType.value())
            val task = armPollTaskFactory!!.newStorageStatusCheckerTask(ac,
                    StorageCheckerContext(ArmCredentialView(ac.cloudCredential), storageGroup, osStorageName, SUCCEEDED))
            syncPollingScheduler!!.schedule(task)
        }
    }

    @Throws(Exception::class)
    fun deleteStorage(authenticatedContext: AuthenticatedContext, client: AzureRMClient, osStorageName: String, storageGroup: String) {
        if (storageAccountExist(client, osStorageName)) {
            client.deleteStorageAccount(storageGroup, osStorageName)
            val task = armPollTaskFactory!!.newStorageStatusCheckerTask(authenticatedContext,
                    StorageCheckerContext(ArmCredentialView(authenticatedContext.cloudCredential), storageGroup, osStorageName, NOTFOUND))
            syncPollingScheduler!!.schedule(task)
        }
    }

    private fun buildStorageName(armAttachedStorageOption: ArmAttachedStorageOption, acv: ArmCredentialView, vmId: Long?, cloudContext: CloudContext,
                                 storageType: ArmDiskType): String {
        var result: String
        var name = cloudContext.name.toLowerCase().replace("\\s+|-".toRegex(), "")
        name = if (name.length > MAX_LENGTH_OF_NAME_SLICE) name.substring(0, MAX_LENGTH_OF_NAME_SLICE) else name
        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val storageAccountId = acv.id!!.toString() + "#" + cloudContext.id + "#" + cloudContext.owner
            LOGGER.info("Storage account internal id: {}", storageAccountId)
            val digest = messageDigest.digest(storageAccountId.toByteArray())
            var paddedId = ""
            if (armAttachedStorageOption == ArmAttachedStorageOption.PER_VM && vmId != null) {
                paddedId = String.format("%3s", java.lang.Long.toString(vmId, RADIX)).replace(' ', '0')
            }
            result = name + storageType.abbreviation + paddedId + BigInteger(1, digest).toString(RADIX)
        } catch (e: NoSuchAlgorithmException) {
            result = name + acv.id + cloudContext.id + cloudContext.owner
        }

        if (result.length > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME)
        }
        LOGGER.info("Storage account name: {}", result)
        return result
    }

    private fun getPersistentStorageName(persistentStorageName: String, acv: ArmCredentialView, region: String): String {
        val subscriptionIdPart = acv.subscriptionId.replace("-".toRegex(), "").toLowerCase()
        val regionInitials = WordUtils.initials(region, ' ').toLowerCase()
        var result = String.format("%s%s%s", persistentStorageName, regionInitials, subscriptionIdPart)
        if (result.length > MAX_LENGTH_OF_RESOURCE_NAME) {
            result = result.substring(0, MAX_LENGTH_OF_RESOURCE_NAME)
        }
        LOGGER.info("Storage account name: {}", result)
        return result
    }

    fun getDiskContainerName(cloudContext: CloudContext): String {
        return armUtils!!.getStackName(cloudContext)
    }

    fun getPersistentStorageName(parameters: Map<String, String>): String {
        return parameters["persistentStorage"]
    }

    fun isPersistentStorage(persistentStorageName: String): Boolean {
        return !Strings.isNullOrEmpty(persistentStorageName)
    }

    fun getImageResourceGroupName(cloudContext: CloudContext, parameters: Map<String, String>): String {
        if (isPersistentStorage(getPersistentStorageName(parameters))) {
            return getPersistentStorageName(parameters)
        }
        return armUtils!!.getResourceGroupName(cloudContext)
    }


    private fun storageAccountExist(client: AzureRMClient, storageName: String): Boolean {
        try {
            val storageAccounts = client.storageAccounts
            for (stringObjectMap in storageAccounts) {
                if (stringObjectMap["name"] == storageName) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }

        return false
    }

    companion object {

        val IMAGES = "images"
        val STORAGE_BLOB_PATTERN = "https://%s.blob.core.windows.net/"

        private val LOGGER = LoggerFactory.getLogger(ArmStorage::class.java)

        private val RADIX = 32
        private val MAX_LENGTH_OF_NAME_SLICE = 8
        private val MAX_LENGTH_OF_RESOURCE_NAME = 24
    }
}


