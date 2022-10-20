package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.PackageVersionChecker;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class SaltBootstrapVersionChecker {

    public static final String CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION = "0.13.6";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrapVersionChecker.class);

    private static final Versioned FINGERPRINT_SUPPORT_MIN_VERSION = () -> "0.13.2";

    private static final Versioned RESTART_NEEDED_FLAG_SUPPORT_MIN_VERSION = () -> "0.13.4";

    public boolean isFingerprintingSupported(Json image) {
        return isSupported(FINGERPRINT_SUPPORT_MIN_VERSION, image);
    }

    public boolean isRestartNeededFlagSupported(Json image) {
        return isSupported(RESTART_NEEDED_FLAG_SUPPORT_MIN_VERSION, image);
    }

    public boolean isChangeSaltuserPasswordSupported(Json image) {
        return isSupported(() -> CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION, image);
    }

    private boolean isSupported(Versioned version, Json image) {
        if (image == null) {
            LOGGER.info("Image is null, couldn't verify salt-bootstrap version");
            return false;
        } else {
            try {
                Image instanceImage = image.get(Image.class);
                Map<String, String> packageVersions = instanceImage.getPackageVersions();
                if (packageVersions != null) {
                    String saltBootstrapVersion = packageVersions.getOrDefault(PackageVersionChecker.SALT_BOOTSTRAP, "0.0.0");
                    Versioned currentVersion = () -> StringUtils.substringBefore(saltBootstrapVersion, "-");
                    LOGGER.debug("Saltboot version in image: {}", currentVersion.getVersion());
                    return -1 < new VersionComparator().compare(currentVersion, version);
                } else {
                    LOGGER.info("PackageVersions is null in image {} {}", instanceImage.getImageId(), instanceImage.getImageName());
                    return false;
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't parse image", e);
                return false;
            }
        }
    }
}
