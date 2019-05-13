package com.sequenceiq.environment.credential.validator;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.account.PreferencesService;
import com.sequenceiq.environment.credential.CredentialDefinitionService;

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
