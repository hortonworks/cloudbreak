package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.ImageStatusResult;

/**
 * Collection of basic methods to prepare the Cloud provider to launch a given stack.
 */
public interface Setup {

    /**
     * Creates the VM if it is not available. Some platforms do not allow to start a VM from a central image but it forces the user to copy the image to
     * its own storage.
     * <br>
     * To check whether the image copy is finished use {@link #checkImageStatus(AuthenticatedContext, CloudStack, Image)}
     *
     * @param authenticatedContext the context which already contains the authenticated client
     * @param stack                stack the definition of infrastructure that needs to be launched
     * @param image                the image to be copied
     * @param prepareImageType     the caller flow, can be either during creation or image change
     * @param fallbackTargetImage  the name of the image to be used in case of Marketplace -> VHD fallback
     */
    void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, Image image, PrepareImageType prepareImageType, String fallbackTargetImage);

    /**
     * Invoked by Cloudbreak to check whether the image copy is finished
     *
     * @param authenticatedContext the context which already contains the authenticated client
     * @param stack                stack the definition of infrastructure that needs to be launched
     * @param image                the image to be copied
     * @return state of the image
     */
    ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, Image image);

    /**
     * Implementation of this method shall contain basic checks, e.g. checking that the flavours defined in {@link CloudStack} available or the
     * platform or checking whether the defined subnet is in the same region where the stack intended to be launched
     *
     * @param authenticatedContext the context which already contains the authenticated client
     * @param stack                stack the definition of infrastructure that needs to be launched
     * @param persistenceNotifier  if a resource has been created during this prerequisite check then the Cloud provider can persist them to Cloudbreak's
     */
    void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier);

    /**
     * Hadoop supports multiple filesystems instead of HDFS. These filesystems can be validated before cluster creation.
     *
     * @param credential credential to enable validation
     * @param spiFileSystem filesystem to validate
     * @throws Exception exception is thrown when the filesystem does not meet the desired requirements
     */
    void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) throws Exception;

    /**
     * Implementation of this method shall validate the parameters if valid or not.
     *
     * @param authenticatedContext the context which already contains the authenticated client
     * @param parameters map of parameters
     */
    void validateParameters(AuthenticatedContext authenticatedContext, Map<String, String> parameters);

    /**
     * Implementation of this method shall contain basic checks if scaling is possible or not.
     *
     * @param authenticatedContext the context which already contains the authenticated client
     * @param stack                stack the definition of infrastructure that needs to be launched
     * @param upscale              true in case of upscale, false in case of downscale
     */
    void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale);

    /**
     * Checks image of it is available to use on the cloud provider, eg. check image terms on Azure
     *
     * @param auth  the context which already contains the authenticated client
     * @param stack stack the definition of infrastructure
     * @param image image to validate
     */
    void validateImage(AuthenticatedContext auth, CloudStack stack, Image image);
}
