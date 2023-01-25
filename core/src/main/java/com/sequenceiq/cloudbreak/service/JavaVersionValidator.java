package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class JavaVersionValidator {

    @Inject
    private EntitlementService entitlementService;

    public void validateImage(Image image, Integer javaVersion, String accountId) {
        if (javaVersion != null) {
            if (!entitlementService.isForcedJavaVersionEnabled(accountId)) {
                throw new BadRequestException("Forcing java version is not supported in your account.");
            } else if (!image.getPackageVersions().containsKey(String.format("java%d", javaVersion))) {
                throw new BadRequestException(String.format("The '%s' image does not support java version %d to be forced.", image.getUuid(), javaVersion));
            }
        }
    }
}
