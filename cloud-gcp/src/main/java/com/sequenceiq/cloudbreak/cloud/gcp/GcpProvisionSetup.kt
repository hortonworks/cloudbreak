package com.sequenceiq.cloudbreak.cloud.gcp

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildCompute
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.buildStorage
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getBucket
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getImageName
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getProjectId
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getTarName

import java.io.IOException
import java.util.Date

import javax.inject.Inject

import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Image
import com.google.api.services.compute.model.ImageList
import com.google.api.services.storage.Storage
import com.google.api.services.storage.model.Bucket
import com.google.api.services.storage.model.StorageObject
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

@Service
class GcpProvisionSetup : Setup {

    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<Boolean>? = null

    override fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: com.sequenceiq.cloudbreak.cloud.model.Image) {
        val stackId = authenticatedContext.cloudContext.id!!
        val credential = authenticatedContext.cloudCredential
        try {
            val projectId = getProjectId(credential)
            val imageName = image.imageName
            val storage = buildStorage(credential, authenticatedContext.cloudContext.name)
            val compute = buildCompute(credential)
            val list = compute.images().list(projectId).execute()
            val time = Date().time
            if (!containsSpecificImage(list, imageName)) {
                try {
                    val bucket = Bucket()
                    bucket.name = projectId + time
                    bucket.storageClass = "STANDARD"
                    val ins = storage.buckets().insert(projectId, bucket)
                    ins.execute()
                } catch (ex: GoogleJsonResponseException) {
                    if (ex.statusCode != HttpStatus.SC_CONFLICT) {
                        throw ex
                    }
                }

                val tarName = getTarName(imageName)
                val copy = storage.objects().copy(getBucket(imageName), tarName, projectId + time, tarName, StorageObject())
                copy.execute()

                val gcpApiImage = Image()
                gcpApiImage.name = getImageName(imageName)
                val rawDisk = Image.RawDisk()
                rawDisk.source = String.format("http://storage.googleapis.com/%s/%s", projectId + time, tarName)
                gcpApiImage.rawDisk = rawDisk
                val ins1 = compute.images().insert(projectId, gcpApiImage)
                ins1.execute()
            }
        } catch (e: Exception) {
            val msg = String.format("Error occurred on %s stack during the setup: %s", stackId, e.message)
            LOGGER.error(msg, e)
            throw CloudConnectorException(msg, e)
        }

    }

    override fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: com.sequenceiq.cloudbreak.cloud.model.Image): ImageStatusResult {
        val credential = authenticatedContext.cloudCredential
        val projectId = getProjectId(credential)
        val imageName = image.imageName
        try {
            val gcpApiImage = Image()
            gcpApiImage.name = getImageName(imageName)
            val compute = buildCompute(credential)
            val getImages = compute.images().get(projectId, gcpApiImage.name)
            val status = getImages.execute().status
            LOGGER.info("Status of image {} copy: {}", gcpApiImage.name, status)
            if (READY == status) {
                return ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED)
            }
        } catch (e: IOException) {
            LOGGER.warn("Failed to retrieve image copy status", e)
            return ImageStatusResult(ImageStatus.CREATE_FAILED, 0)
        }

        return ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF)
    }

    override fun prerequisites(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {
        LOGGER.debug("setup has been executed")
    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {
    }

    private fun containsSpecificImage(imageList: ImageList, imageUrl: String): Boolean {
        try {
            for (image in imageList.items) {
                if (image.name == getImageName(imageUrl)) {
                    return true
                }
            }
        } catch (ex: NullPointerException) {
            return false
        }

        return false
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(GcpProvisionSetup::class.java)
        private val READY = "READY"
    }
}
