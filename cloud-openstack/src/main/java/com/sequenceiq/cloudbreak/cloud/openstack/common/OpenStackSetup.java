package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.image.v2.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

@Component
public class OpenStackSetup implements Setup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackSetup.class);

    @Value("${cb.os.enable.autoimport}")
    private Boolean autoImport;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackImageVerifier openStackImageVerifier;

    @Inject
    private OpenStackImageImporter openStackImageImporter;

    @Inject
    private OpenStackFlavorVerifier openStackFlavorVerifier;

    @Override
    public void prepareImage(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        String imageName = image.getImageName();
        OSClient<?> osClient = openStackClient.createOSClient(authenticatedContext);
        if (!openStackImageVerifier.exist(osClient, imageName)) {
            if (autoImport) {
                openStackImageImporter.importImage(osClient, imageName);
            } else {
                throw new CloudConnectorException(String.format("OpenStack image: %s not found", imageName));
            }
        }
    }

    @Override
    public ImageStatusResult checkImageStatus(AuthenticatedContext authenticatedContext, CloudStack stack, com.sequenceiq.cloudbreak.cloud.model.Image image) {
        String imageName = image.getImageName();
        OSClient osClient = openStackClient.createOSClient(authenticatedContext);
        Optional<Image.ImageStatus> optionalImageStatus = openStackImageVerifier.getStatus(osClient, imageName);

        ImageStatusResult imageStatusResult = optionalImageStatus.map(imageStatus -> {
            switch (imageStatus) {
                case ACTIVE:
                    return new ImageStatusResult(ImageStatus.CREATE_FINISHED, ImageStatusResult.COMPLETED);
                case QUEUED:
                case SAVING:
                    return new ImageStatusResult(ImageStatus.IN_PROGRESS, ImageStatusResult.HALF);
                default:
                    return new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED);
            }
        }).orElse(
                new ImageStatusResult(ImageStatus.CREATE_FAILED, ImageStatusResult.COMPLETED)
        );

        LOGGER.info("OpenStack image result. name: {}, imageStatus: {}, imageStatusResult: {}", imageName, optionalImageStatus.orElse(null), imageStatusResult);
        return imageStatusResult;
    }

    @Override
    public void prerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier) {
        OSClient<?> osClient = openStackClient.createOSClient(authenticatedContext);
        openStackFlavorVerifier.flavorsExist(osClient, stack.getGroups());
        LOGGER.debug("setup has been executed");
    }

    @Override
    public void validateFileSystem(CloudCredential credential, SpiFileSystem spiFileSystem) {
    }

    @Override
    public void validateParameters(AuthenticatedContext authenticatedContext, Map<String, String> parameters) throws Exception {

    }

    @Override
    public void scalingPrerequisites(AuthenticatedContext authenticatedContext, CloudStack stack, boolean upscale) {

    }
}
