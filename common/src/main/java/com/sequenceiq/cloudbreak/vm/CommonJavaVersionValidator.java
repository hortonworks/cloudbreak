package com.sequenceiq.cloudbreak.vm;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class CommonJavaVersionValidator {

    @Inject
    private VirtualMachineConfiguration virtualMachineConfiguration;

    public void validateByVmConfiguration(String runtime, Integer javaVersion) {
        if (StringUtils.isEmpty(runtime) || !virtualMachineConfiguration.getSupportedJavaVersionsByRuntime().containsKey(runtime)) {
            if (!virtualMachineConfiguration.isJavaVersionSupported(javaVersion)) {
                throw new BadRequestException(String.format("Java version %d is not supported.", javaVersion));
            }
        } else if (!virtualMachineConfiguration.isJavaVersionSupported(runtime, javaVersion)) {
            throw new BadRequestException(String.format("Java version %d is not supported for runtime %s.", javaVersion, runtime));
        }
    }
}
