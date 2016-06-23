package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.FileSystem
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult

/**
 * Collection of basic methods to prepare the Cloud provider to launch a given stack.
 */
interface Setup {

    /**
     * Creates the VM if it is not available. Some platform does not allow to start a VM from a central image but it forces the user to copy the image to
     * its own storage.
     *
     *
     * To check whether the image copy is finished use [.checkImageStatus]

     * @param authenticatedContext the context which already contains the authenticated client
     * *
     * @param stack                stack the definition of infrastucture that needs to be launched
     * *
     * @param image                the image to be copied
     */
    fun prepareImage(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image)

    /**
     * Invoked by Cloudbreak to check the whether the image copy is finished

     * @param authenticatedContext the context which already contains the authenticated client
     * *
     * @param stack                stack the definition of infrastucture that needs to be launched
     * *
     * @param image                the image to be copied
     * *
     * @return state of the image
     */
    fun checkImageStatus(authenticatedContext: AuthenticatedContext, stack: CloudStack, image: Image): ImageStatusResult

    /**
     * Implementation of this method shall contain basic checks, e.g. checking that the the flavours defined in [CloudStack] available or the
     * platform or checking whether the defined subnet is in the same region where the stack intended to be launched

     * @param authenticatedContext the context which already contains the authenticated client
     * *
     * @param stack                stack the definition of infrastucture that needs to be launched
     * *
     * @param persistenceNotifier  if a resource has been created during this prerequisit check then the Cloud provider can persist them to Cloudbreak's
     */
    fun prerequisites(authenticatedContext: AuthenticatedContext, stack: CloudStack, persistenceNotifier: PersistenceNotifier)

    /**
     * Hadoop supports multiple filesystems instead of HDFS. These filesystems can be validated before cluster creation.

     * @param fileSystem filesystem to validate
     * *
     * @throws Exception exception is thrown when the filesystem does not meet the desired requirements
     */
    @Throws(Exception::class)
    fun validateFileSystem(fileSystem: FileSystem)

}
