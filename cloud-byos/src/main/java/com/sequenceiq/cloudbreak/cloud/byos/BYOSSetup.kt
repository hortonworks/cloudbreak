package com.sequenceiq.cloudbreak.cloud.byos

import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

class BYOSSetup : Setup {
    override fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image) {

    }

    override fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image): ImageStatusResult {
        return null
    }

    override fun prerequisites(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier) {

    }

    @Throws(Exception::class)
    override fun validateFileSystem(fileSystem: FileSystem) {

    }
}
