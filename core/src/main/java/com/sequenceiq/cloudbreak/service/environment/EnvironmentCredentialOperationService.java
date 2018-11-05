package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.environment.request.CredentialAwareEnvRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Service
public class EnvironmentCredentialOperationService {

    @Inject
    private CredentialService credentialService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public Credential getCredentialFromRequest(CredentialAwareEnvRequest request, Long workspaceId) {
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

    public Credential validatePlatformAndGetCredential(CredentialAwareEnvRequest request, Environment environment, Long workspaceId) {
        String requestedPlatform;
        if (StringUtils.isNotEmpty(request.getCredentialName())) {
            Credential credential = credentialService.getByNameForWorkspaceId(request.getCredentialName(), workspaceId);
            requestedPlatform = credential.cloudPlatform();
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
