package com.sequenceiq.cloudbreak.service.java;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;

@Service
public class JavaVersionValidator {

    private static final int JAVA_VERSION_8 = 8;

    @Inject
    private VirtualMachineConfiguration virtualMachineConfiguration;

    public void validateImage(Image image, Integer javaVersion) {
        if (javaVersion != null) {
            if (!virtualMachineConfiguration.getSupportedJavaVersions().contains(javaVersion)) {
                throw new BadRequestException(String.format("Java version %d is not supported.", javaVersion));
            }
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
        throw new BadRequestException(String.format("The '%s' image does not support java version %d to be forced.", uuid, javaVersion));
    }

    private boolean javaPresentedInMetadata(Image image) {
        return image.getPackageVersion(ImagePackageVersion.JAVA) != null;
    }
}
