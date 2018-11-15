package com.sequenceiq.cloudbreak.controller.validation.credential;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;

@Component
public class CredentialValidator {

    @Inject
    private PreferencesService preferencesService;

    public void validateCredentialCloudPlatform(String cloudPlatform) {
        if (!preferencesService.enabledPlatforms().contains(cloudPlatform)) {
            throw new BadRequestException(String.format("There is no such cloud platform as '%s'", cloudPlatform));
        }
    }

}
