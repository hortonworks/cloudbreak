package com.sequenceiq.freeipa.util;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageCatalogException;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class AvailabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityChecker.class);

    @Inject
    private ImageService imageService;

    @Inject
    private StackService stackService;

    public boolean isRequiredPackagesInstalled(Stack stack, Set<String> requiredPackages) {
        try {
            Image image = imageService.getImageForStack(stack);
            if (image != null) {
                Map<String, String> packageVersions = image.getPackageVersions();
                if (packageVersions != null) {
                    boolean requiredPackagesInstalled = requiredPackages == null || packageVersions.keySet().containsAll(requiredPackages);
                    LOGGER.debug("Required packages {} installed {}", requiredPackages, requiredPackagesInstalled);
                    return requiredPackagesInstalled;
                } else {
                    LOGGER.warn("PackageVersions is null in image {}", image.getUuid());
                    return false;
                }
            } else {
                LOGGER.warn("Image not found");
                return false;
            }
        } catch (ImageNotFoundException | ImageCatalogException e) {
            LOGGER.warn("Image not found", e);
            return false;
        }
    }

    public boolean isPackageAvailable(Long stackId, String packageName, Versioned supportedAfter) {
        Stack stack = stackService.getStackById(stackId);
        return isPackageAvailable(stack, packageName, supportedAfter);
    }

    protected boolean isAvailable(Stack stack, Versioned supportedAfter) {
        if (StringUtils.isNotBlank(stack.getAppVersion())) {
            Versioned currentVersion = stack::getAppVersion;
            return new VersionComparator().compare(currentVersion, supportedAfter) > 0;
        } else {
            return false;
        }
    }

    protected boolean isPackageAvailable(Stack stack, String packageName, Versioned version) {
        try {
            Image image = imageService.getImageForStack(stack);
            return isPackageAvailable(packageName, version, image);
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found for stack {}", stack.getResourceCrn(), e);
            return false;
        }
    }

    protected boolean isPackageAvailable(String packageName, Versioned version, Image image) {
        if (image != null) {
            Map<String, String> packageVersions = image.getPackageVersions();
            if (packageVersions != null) {
                String packageVersion = packageVersions.getOrDefault(packageName, "0.0.0");
                Versioned currentVersion = () -> StringUtils.substringBefore(packageVersion, "-");
                LOGGER.debug("Package {} version in image: {}", packageName, currentVersion.getVersion());
                return new VersionComparator().compare(currentVersion, version) > 0;
            } else {
                LOGGER.warn("PackageVersions is null in image {}", image.getUuid());
                return false;
            }
        } else {
            LOGGER.warn("Image not found");
            return false;
        }
    }

    protected ImageService getImageService() {
        return imageService;
    }

    protected boolean doesAllImageSupport(Stack stack, String packageName, Versioned supportedAfter) {
        Set<String> imageIdsInUse = stack.getNotTerminatedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getImage)
                .map(imageJson -> imageJson.getUnchecked(com.sequenceiq.cloudbreak.cloud.model.Image.class))
                .map(com.sequenceiq.cloudbreak.cloud.model.Image::getImageId)
                .collect(Collectors.toSet());
        String currentImageId = getImageService().getImageForStack(stack).getUuid();
        imageIdsInUse.add(currentImageId);
        return imageIdsInUse.size() == 1
                || doesAllNonCurrentImageSupport(stack, currentImageId, imageIdsInUse, packageName, supportedAfter);
    }

    private boolean doesAllNonCurrentImageSupport(Stack stack, String currentImageId, Set<String> imageIdsInUse, String packageName,
            Versioned supportedAfter) {
        imageIdsInUse.remove(currentImageId);
        FreeIpaImageFilterSettings imageFilterSettings = getImageService().createImageFilterSettingsFromImageEntity(stack);
        try {
            return imageIdsInUse.stream()
                    .map(imageFilterSettings::withImageId)
                    .map(imageService::getImage)
                    .map(ImageWrapper::getImage)
                    .allMatch(image -> isPackageAvailable(packageName, supportedAfter, image));
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found, feature support cannot be determined, returning false");
            return false;
        }
    }
}
