package com.sequenceiq.environment.environment.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.api.environment.model.request.CredentialAwareEnvV1Request;
import com.sequenceiq.environment.credential.Credential;
import com.sequenceiq.environment.credential.CredentialService;
import com.sequenceiq.environment.environment.domain.Environment;

@Service
public class EnvironmentCredentialOperationService {

    @Inject
    private CredentialService credentialService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public Credential getCredentialFromRequest(CredentialAwareEnvV1Request request, Long workspaceId) {
        Credential credential;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            try {
                credential = credentialService.getByNameForWorkspaceId(request.getCredentialName(), workspaceId);
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        request.getCredentialName()), e);
            }
        } else {
            Credential converted = conversionService.convert(request.getCredential(), Credential.class);
            credential = credentialService.createForLoggedInUser(converted, workspaceId);
        }
        return credential;
    }

    public Credential validatePlatformAndGetCredential(CredentialAwareEnvV1Request request, Environment environment, Long workspaceId) {
        String requestedPlatform;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            Credential credential = credentialService.getByNameForWorkspaceId(request.getCredentialName(), workspaceId);
            requestedPlatform = credential.getCloudPlatform();
            validatePlatform(environment, requestedPlatform);
            return credential;
        } else {
            requestedPlatform = request.getCredential().getCloudPlatform();
            validatePlatform(environment, requestedPlatform);
            Credential converted = conversionService.convert(request.getCredential(), Credential.class);
            return credentialService.createForLoggedInUser(converted, workspaceId);
        }
    }

    private void validatePlatform(Environment environment, String requestedPlatform) {
        if (!environment.getCloudPlatform().equals(requestedPlatform)) {
            throw new BadRequestException(String.format("The requested credential's cloud platform [%s] "
                    + "does not match with the environments cloud platform [%s].", requestedPlatform, environment.getCloudPlatform()));
        }
    }
}
