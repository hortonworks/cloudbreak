package com.sequenceiq.freeipa.util;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class SaltBootstrapVersionChecker {

    public static final String SALT_BOOTSTRAP_PACKACE = "salt-bootstrap";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrapVersionChecker.class);

    private static final Versioned CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION = () -> "0.13.6";

    @Inject
    private ImageService imageService;

    public boolean isChangeSaltuserPasswordSupported(Stack stack) {
        return isSupported(CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION, stack);
    }

    private boolean isSupported(Versioned version, Stack stack) {
        Image image = imageService.getImageForStack(stack);
        if (image != null) {
            Map<String, String> packageVersions = image.getPackageVersions();
            if (packageVersions != null) {
                String saltBootstrapVersion = packageVersions.getOrDefault(SALT_BOOTSTRAP_PACKACE, "0.0.0");
                Versioned currentVersion = () -> StringUtils.substringBefore(saltBootstrapVersion, "-");
                LOGGER.debug("Saltboot version in image: {}", currentVersion.getVersion());
                return -1 < new VersionComparator().compare(currentVersion, version);
            } else {
                LOGGER.warn("PackageVersions is null in image {}", image.getUuid());
            }
        } else {
            LOGGER.warn("Image not found for stack {}", stack.getResourceCrn());
        }
        return false;
    }
}
