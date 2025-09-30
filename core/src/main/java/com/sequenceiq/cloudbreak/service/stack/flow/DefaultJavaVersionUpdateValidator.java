package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.lang.Integer.parseInt;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.java.vm.AllowableJavaConfigurations;

@Service
public class DefaultJavaVersionUpdateValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJavaVersionUpdateValidator.class);

    private static final String RELEASE_VERSION = "release-version";

    @Inject
    private AllowableJavaConfigurations allowableJavaConfigurations;

    @Inject
    private ImageService imageService;

    public void validate(StackDto stack, SetDefaultJavaVersionRequest javaVersionRequest) {
        LOGGER.debug("Validating the update of Java version to '{}', on stack '{}'", javaVersionRequest.getDefaultJavaVersion(), stack.getName());
        try {
            Image image = imageService.getImage(stack.getId());
            validateTheRequestedJavaVersionExistenceOnTheImage(image, javaVersionRequest.getDefaultJavaVersion());
            String runtimeVersion = getRuntimeVersion(stack, image);
            if (StringUtils.isNotEmpty(runtimeVersion)) {
                allowableJavaConfigurations.checkValidConfiguration(parseInt(javaVersionRequest.getDefaultJavaVersion()), runtimeVersion);
            } else {
                LOGGER.warn("The runtime version could not be found on the VM image('{}') of the cluster with name '{}'.", image.getImageId(), stack.getName());
            }
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            String message = String.format("Image information could not be found for the cluster with name '%s'", stack.getName());
            LOGGER.warn(message);
            throw new BadRequestException(message, e);
        }
    }

    public List<String> listAvailableJavaVersions(StackDto stack) {
        try {
            Image image = imageService.getImage(stack.getId());
            String runtimeVersion = getRuntimeVersion(stack, image);
            return allowableJavaConfigurations.listValidJavaVersions(runtimeVersion);
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            String message = String.format("Image information could not be found for the cluster with name '%s'", stack.getName());
            LOGGER.warn(message);
            throw new BadRequestException(message, e);
        }
    }

    private String getRuntimeVersion(StackDto stack, Image image) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        String releaseVersion = image.getTags().get(RELEASE_VERSION);
        if (StringUtils.isEmpty(releaseVersion)) {
            StatedImage currentStatedImage = imageService.getCurrentImage(stack.getWorkspaceId(), stack.getId());
            releaseVersion = currentStatedImage.getImage().getTags().get(RELEASE_VERSION);
        }
        if (StringUtils.isEmpty(releaseVersion)) {
            return image.getPackageVersions().get(ImagePackageVersion.STACK.getKey());
        }
        return releaseVersion;
    }

    private void validateTheRequestedJavaVersionExistenceOnTheImage(Image image, String defaultJavaVersion) {
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
    }
}
