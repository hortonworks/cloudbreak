package com.sequenceiq.freeipa.util;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageService;

@Component
public class AvailabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityChecker.class);

    @Inject
    private ImageService imageService;

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
            if (image != null) {
                Map<String, String> packageVersions = image.getPackageVersions();
                if (packageVersions != null) {
                    String packageVersion = packageVersions.getOrDefault(packageName, "0.0.0");
                    Versioned currentVersion = () -> StringUtils.substringBefore(packageVersion, "-");
                    LOGGER.debug("Package {} version in image: {}", packageName, currentVersion.getVersion());
                    return new VersionComparator().compare(currentVersion, version) > 0;
                } else {
                    LOGGER.warn("PackageVersions is null in image {}", image.getUuid());
                }
            } else {
                LOGGER.warn("Image not found for stack {}", stack.getResourceCrn());
            }
        } catch (ImageNotFoundException e) {
            LOGGER.warn("Image not found for stack {}", stack.getResourceCrn(), e);
        }
        return false;
    }

}
