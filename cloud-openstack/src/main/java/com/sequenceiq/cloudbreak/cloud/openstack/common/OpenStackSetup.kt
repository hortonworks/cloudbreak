package com.sequenceiq.cloudbreak.cloud.openstack.common

import java.util.HashSet

import javax.inject.Inject

import org.openstack4j.api.OSClient
import org.openstack4j.model.compute.Flavor
import org.openstack4j.model.image.Image
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

@Component
class OpenStackSetup : Setup {

    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: com.sequenceiq.cloudbreak.cloud.model.Image) {
        val imageName = image.imageName
        val osClient = openStackClient!!.createOSClient(authenticatedContext)
        verifyImage(osClient, imageName)
    }

    override fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: com.sequenceiq.cloudbreak.cloud.model.Image): ImageStatusResult {
        return ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED)
    }

    override fun prerequisites(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {
        val osClient = openStackClient!!.createOSClient(authenticatedContext)
        verifyFlavors(osClient, stack.groups)
        LOGGER.debug("setup has been executed")
    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {
    }

    private fun verifyFlavors(osClient: OSClient, instanceGroups: List<Group>) {
        val flavors = osClient.compute().flavors().list()
        val notFoundFlavors = HashSet<String>()
        for (instanceGroup in instanceGroups) {
            val instanceType = instanceGroup.instances[0].template!!.flavor
            var found = false
            for (flavor in flavors) {
                if (flavor.name.equals(instanceType, ignoreCase = true)) {
                    found = true
                    break
                }
            }
            if (!found) {
                notFoundFlavors.add(instanceType)
            }
        }

        if (!notFoundFlavors.isEmpty()) {
            throw CloudConnectorException(String.format("Not found flavors: %s", notFoundFlavors))
        }
    }

    private fun verifyImage(osClient: OSClient, name: String) {
        val images = osClient.images().list()
        for (image in images) {
            if (name.equals(image.name, ignoreCase = true)) {
                return
            }
        }
        throw CloudConnectorException(String.format("OpenStack image: %s not found", name))
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackSetup::class.java)
    }
}
