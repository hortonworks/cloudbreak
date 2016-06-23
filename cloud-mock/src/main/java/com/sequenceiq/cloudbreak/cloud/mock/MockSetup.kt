package com.sequenceiq.cloudbreak.cloud.mock

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

@Service
class MockSetup : Setup {

    override fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image) {
        LOGGER.debug("prepare image has been executed")
    }

    override fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image): ImageStatusResult {
        return ImageStatusResult(ImageStatus.CREATE_FINISHED, FINISHED_PROGRESS_VALUE)
    }

    override fun prerequisites(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {
        LOGGER.debug("setup prerequisites invoked..")
    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockSetup::class.java)
        private val FINISHED_PROGRESS_VALUE = 100
    }
}
