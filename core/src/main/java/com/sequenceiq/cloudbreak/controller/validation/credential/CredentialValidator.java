package com.sequenceiq.cloudbreak.controller.validation.credential;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudPlarformService;

import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CredentialValidator {

    @Inject
    private CloudPlarformService cloudPlarformService;

    public void validateCredentialCloudPlatform(String cloudPlatform) {
        if (!cloudPlarformService.enabledPlatforms().contains(cloudPlatform)) {
            throw new BadRequestException(String.format("There is no such cloud platform as '%s'", cloudPlatform));
        }
    }

}
