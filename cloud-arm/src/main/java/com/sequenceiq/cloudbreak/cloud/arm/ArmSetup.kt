package com.sequenceiq.cloudbreak.cloud.arm

import com.sequenceiq.cloudbreak.cloud.arm.ArmStorage.IMAGES
import com.sequenceiq.cloudbreak.cloud.arm.ArmUtils.NOT_FOUND

import java.net.URISyntaxException
import java.net.UnknownHostException

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CopyState
import com.microsoft.azure.storage.blob.CopyStatus
import com.microsoft.azure.storage.blob.ListBlobItem
import com.sequenceiq.cloud.azure.client.AzureRMClient
import com.sequenceiq.cloudbreak.api.model.WasbFileSystemConfiguration
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.arm.task.ArmPollTaskFactory
import com.sequenceiq.cloudbreak.cloud.arm.view.ArmCredentialView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult
import com.sequenceiq.cloudbreak.common.type.ResourceType

import groovyx.net.http.HttpResponseException

@Component
class ArmSetup : Setup {

    @Inject
    private val armClient: ArmClient? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null
    @Inject
    private val armUtils: ArmUtils? = null
    @Inject
    private val armPollTaskFactory: ArmPollTaskFactory? = null
    @Inject
    private val armStorage: ArmStorage? = null

    override fun prepareImage(ac: AuthenticatedContext, stack: CloudStack, image: Image) {
        LOGGER.info("prepare image: {}", image)
        val acv = ArmCredentialView(ac.cloudCredential)
        val imageStorageName = armStorage!!.getImageStorageName(acv, ac.cloudContext, armStorage.getPersistentStorageName(stack.parameters),
                armStorage.getArmAttachedStorageOption(stack.parameters))
        val resourceGroupName = armUtils!!.getResourceGroupName(ac.cloudContext)
        val imageResourceGroupName = armStorage.getImageResourceGroupName(ac.cloudContext, stack.parameters)
        val client = armClient!!.getClient(ac.cloudCredential)
        val region = ac.cloudContext.location!!.region.value()
        try {
            if (!resourceGroupExist(client, resourceGroupName)) {
                client.createResourceGroup(resourceGroupName, region)
            }
            if (!resourceGroupExist(client, imageResourceGroupName)) {
                client.createResourceGroup(imageResourceGroupName, region)
            }
            armStorage.createStorage(ac, client, imageStorageName, ArmDiskType.LOCALLY_REDUNDANT, imageResourceGroupName, region)
            client.createContainerInStorage(imageResourceGroupName, imageStorageName, IMAGES)
            if (!storageContainsImage(client, imageResourceGroupName, imageStorageName, image.imageName)) {
                client.copyImageBlobInStorageContainer(imageResourceGroupName, imageStorageName, IMAGES, image.imageName)
            }
        } catch (ex: HttpResponseException) {
            throw CloudConnectorException(ex.response.data.toString(), ex)
        } catch (ex: Exception) {
            throw CloudConnectorException(ex)
        }

        LOGGER.debug("prepare image has been executed")
    }

    override fun checkImageStatus(ac: AuthenticatedContext, stack: CloudStack, image: Image): ImageStatusResult {
        val acv = ArmCredentialView(ac.cloudCredential)
        val imageStorageName = armStorage!!.getImageStorageName(acv, ac.cloudContext, armStorage.getPersistentStorageName(stack.parameters),
                armStorage.getArmAttachedStorageOption(stack.parameters))
        val imageResourceGroupName = armStorage.getImageResourceGroupName(ac.cloudContext, stack.parameters)
        val armCredentialView = ArmCredentialView(ac.cloudCredential)
        val client = armClient!!.getClient(armCredentialView)
        try {
            val copyState = client.getCopyStatus(imageResourceGroupName, imageStorageName, IMAGES, image.imageName)
            if (CopyStatus.SUCCESS == copyState.status) {
                return ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED)
            } else if (CopyStatus.ABORTED == copyState.status || CopyStatus.INVALID == copyState.status) {
                return ImageStatusResult(ImageStatus.CREATE_FAILED, 0)
            } else {
                val percentage = (copyState.bytesCopied as Double * ImageStatusResult.COMPLETED / copyState.totalBytes as Double).toInt()
                LOGGER.info(String.format("CopyStatus Pending %s byte/%s byte: %.4s %%", copyState.totalBytes, copyState.bytesCopied, percentage))
                return ImageStatusResult(ImageStatus.IN_PROGRESS, percentage)
            }
        } catch (e: HttpResponseException) {
            if (e.statusCode != NOT_FOUND) {
                throw CloudConnectorException(e.response.data.toString())
            } else {
                return ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF)
            }
        } catch (ex: Exception) {
            return ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF)
        }

    }

    override fun prerequisites(ac: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {
        val storageGroup = armUtils!!.getResourceGroupName(ac.cloudContext)
        val client = armClient!!.getClient(ac.cloudCredential)
        val cloudResource = CloudResource.Builder().type(ResourceType.ARM_TEMPLATE).name(storageGroup).build()
        val region = ac.cloudContext.location!!.region.value()
        try {
            persistenceNotifier.notifyAllocation(cloudResource, ac.cloudContext)
            if (!resourceGroupExist(client, storageGroup)) {
                client.createResourceGroup(storageGroup, region)
            }
        } catch (ex: HttpResponseException) {
            throw CloudConnectorException(ex.response.data.toString(), ex)
        } catch (ex: Exception) {
            throw CloudConnectorException(ex)
        }

        LOGGER.debug("setup has been executed")
    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {
        val fileSystemType = fileSystem.type
        val accountName = fileSystem.getParameter<String>(WasbFileSystemConfiguration.ACCOUNT_NAME, String::class.java)
        val accountKey = fileSystem.getParameter<String>(WasbFileSystemConfiguration.ACCOUNT_KEY, String::class.java)
        val connectionString = "DefaultEndpointsProtocol=https;AccountName=$accountName;AccountKey=$accountKey"
        val storageAccount = CloudStorageAccount.parse(connectionString)
        val blobClient = storageAccount.createCloudBlobClient()
        val containerReference = blobClient.getContainerReference(TEST_CONTAINER + System.nanoTime())
        try {
            containerReference.createIfNotExists()
            containerReference.delete()
            if (DASH == fileSystemType) {
                throw CloudConnectorException("The provided account belongs to a single storage account, but the selected file system is WASB with DASH")
            }
        } catch (e: StorageException) {
            if (DASH != fileSystemType && e.cause is UnknownHostException) {
                throw CloudConnectorException("The provided account does not belong to a valid storage account")
            }
        }

    }


    private fun resourceGroupExist(client: AzureRMClient, groupName: String): Boolean {
        try {
            val resourceGroups = client.resourceGroups
            for (resourceGroup in resourceGroups) {
                if (resourceGroup["name"] == groupName) {
                    return true
                }
            }

        } catch (e: Exception) {
            return false
        }

        return false
    }

    @Throws(URISyntaxException::class, StorageException::class)
    private fun storageContainsImage(client: AzureRMClient, groupName: String, storageName: String, image: String): Boolean {
        val listBlobItems = client.listBlobInStorage(groupName, storageName, IMAGES)
        for (listBlobItem in listBlobItems) {
            if (getNameFromConnectionString(listBlobItem.uri.path) == image.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[image.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size - 1]) {
                return true
            }
        }
        return false
    }

    private fun getNameFromConnectionString(connection: String): String {
        return connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[connection.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size - 1]
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ArmSetup::class.java)
        private val TEST_CONTAINER = "cb-test-container"
        private val DASH = "DASH"
    }

}
