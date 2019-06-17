package com.sequenceiq.environment.credential.validation;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.configuration.EnabledPlatformProvider;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@Component
public class CredentialValidator {

    @Inject
    private EnabledPlatformProvider enabledPlatformProvider;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    public void validateParameters(Platform platform, Json json) {
        credentialDefinitionService.checkProperties(platform, json);
    }

    public void validateCredentialCloudPlatform(String cloudPlatform) {
        if (!enabledPlatformProvider.enabledPlatforms().contains(cloudPlatform)) {
            throw new BadRequestException(String.format("There is no such cloud platform as '%s'", cloudPlatform));
        }
    }

}
