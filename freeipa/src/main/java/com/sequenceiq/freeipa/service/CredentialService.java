package com.sequenceiq.freeipa.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.freeipa.dto.Credential;

@Service
public class CredentialService {

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private SecretService secretService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public Credential getCredentialByEnvCrn(String envCrn) {
        CredentialResponse credentialResponse = null;
        try {
            credentialResponse = credentialEndpoint.getByEnvironmentCrn(envCrn);
        } catch (ClientErrorException e) {
            try (Response response = e.getResponse()) {
                if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    throw new BadRequestException(String.format("Credential not found by environment CRN: %s", envCrn), e);
                }
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                throw new CloudbreakServiceException(String.format("Failed to get credential: %s", errorMessage), e);
            }
        }
        return convertToCredential(credentialResponse);
    }

    public Credential getCredentialByCredCrn(String credentialCrn) {
        CredentialResponse credentialResponse = null;
        try {
            credentialResponse = credentialEndpoint.getByResourceCrn(credentialCrn);
        } catch (ClientErrorException e) {
            try (Response response = e.getResponse()) {
                if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    throw new BadRequestException(String.format("Credential not found by credential CRN: %s", credentialCrn), e);
                }
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                throw new CloudbreakServiceException(String.format("Failed to get credential: %s", errorMessage), e);
            }
        }
        return convertToCredential(credentialResponse);
    }

    private Credential convertToCredential(CredentialResponse credentialResponse) {
        SecretResponse secretResponse = credentialResponse.getAttributes();
        String attributes = secretService.getByResponse(secretResponse);
        return new Credential(
                credentialResponse.getCloudPlatform(),
                credentialResponse.getName(),
                attributes,
                credentialResponse.getCrn(),
                credentialResponse.getAccountId());
    }
}
