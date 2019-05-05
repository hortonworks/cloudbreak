package com.sequenceiq.cloudbreak.controller.validation.credential;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;

@Component
public class CredentialValidator {

    @Inject
    private PreferencesService preferencesService;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    public void validateParameters(Platform platform, Map<String, Object> parameters) {
        credentialDefinitionService.checkProperties(platform, parameters);
    }

    public void validateCredentialCloudPlatform(String cloudPlatform) {
        if (!preferencesService.enabledPlatforms().contains(cloudPlatform)) {
            throw new BadRequestException(String.format("There is no such cloud platform as '%s'", cloudPlatform));
        }
    }

}
