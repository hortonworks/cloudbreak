package com.sequenceiq.environment.credential.validation;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.validation.definition.CredentialDefinitionService;

@Component
public class CredentialValidator {

    @Value("${environment.enabledplatforms}")
    private Set<String> enabledPlatforms;

    @Inject
    private CredentialDefinitionService credentialDefinitionService;

    public void validateParameters(Platform platform, Json json) {
        credentialDefinitionService.checkProperties(platform, json);
    }

    public void validateCredentialCloudPlatform(String cloudPlatform) {
        if (!enabledPlatforms.contains(cloudPlatform)) {
            throw new BadRequestException(String.format("There is no such cloud platform as '%s'", cloudPlatform));
        }
    }

}
