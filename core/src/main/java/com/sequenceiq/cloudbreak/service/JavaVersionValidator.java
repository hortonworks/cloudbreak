package com.sequenceiq.cloudbreak.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;

@Service
public class JavaVersionValidator {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private VirtualMachineConfiguration virtualMachineConfiguration;

    public void validateImage(Image image, Integer javaVersion, String accountId) {
        if (javaVersion != null) {
            if (!virtualMachineConfiguration.getSupportedJavaVersions().contains(javaVersion)) {
                throw new BadRequestException(String.format("Java version %d is not supported.", javaVersion));
            } else if (!image.getPackageVersions().containsKey(String.format("java%d", javaVersion))) {
                throw new BadRequestException(String.format("The '%s' image does not support java version %d to be forced.", image.getUuid(), javaVersion));
            }
        }
    }
}
