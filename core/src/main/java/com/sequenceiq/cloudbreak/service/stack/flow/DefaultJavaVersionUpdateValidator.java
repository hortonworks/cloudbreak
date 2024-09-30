package com.sequenceiq.cloudbreak.service.stack.flow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Service
public class DefaultJavaVersionUpdateValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJavaVersionUpdateValidator.class);

    @Inject
    private ImageService imageService;

    public void validate(StackDto stack, SetDefaultJavaVersionRequest javaVersionRequest) {
        LOGGER.debug("Validating the update of Java version to '{}', on stack '{}'", javaVersionRequest.getDefaultJavaVersion(), stack.getName());
        validateTheRequestedJavaVersionExistenceOnTheImage(stack, javaVersionRequest.getDefaultJavaVersion());
    }

    private void validateTheRequestedJavaVersionExistenceOnTheImage(StackDto stack, String defaultJavaVersion) {
        String stackName = stack.getName();
        try {
            Image image = imageService.getImage(stack.getId());
            String versionedJavaImagePackageVersion = ImagePackageVersion.JAVA.getKey() + defaultJavaVersion;
            if (image.getPackageVersions().containsKey(versionedJavaImagePackageVersion)) {
                LOGGER.info("Image('{}') contains the requested Java version '{}' with package version key '{}' and version '{}'.", image.getImageId(),
                        defaultJavaVersion, versionedJavaImagePackageVersion, image.getPackageVersions().get(versionedJavaImagePackageVersion));
            } else {
                String message = String.format("The requested Java version '%s' could not be found on the VM image('%s') of the cluster.",
                        defaultJavaVersion, image.getImageId());
                LOGGER.info(message);
                throw new BadRequestException(message);
            }
        } catch (CloudbreakImageNotFoundException e) {
            String message = String.format("Image information could not be found for the cluster with name '%s'", stackName);
            LOGGER.warn(message);
            throw new BadRequestException(message, e);
        }
    }
}
