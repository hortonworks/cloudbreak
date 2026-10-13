package com.sequenceiq.cloudbreak.service.java;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;

@Service
public class JavaVersionValidator {

    private static final int JAVA_VERSION_8 = 8;

    @Inject
    private JavaVersionPolicy javaVersionPolicy;

    public void validateImage(Image image, String runtimeVersion, Integer javaVersion) {
        if (javaVersion != null) {
            javaVersionPolicy.validateSupported(runtimeVersion, javaVersion);
            if (javaPresentedInMetadata(image)) {
                if (!image.getPackageVersions().containsKey(String.format("java%d", javaVersion))) {
                    throw javaVersionNotSupporter(image.getUuid(), javaVersion);
                }
            } else {
                if (javaVersion != JAVA_VERSION_8) {
                    throw javaVersionNotSupporter(image.getUuid(), javaVersion);
                }
            }
        }
    }

    private BadRequestException javaVersionNotSupporter(String uuid, Integer javaVersion) {
        throw new BadRequestException(String.format("The '%s' image does not contain Java version %d. "
                + "Choose an image that contains it, or request a different Java version.", uuid, javaVersion));
    }

    private boolean javaPresentedInMetadata(Image image) {
        return image.getPackageVersion(ImagePackageVersion.JAVA) != null;
    }
}
