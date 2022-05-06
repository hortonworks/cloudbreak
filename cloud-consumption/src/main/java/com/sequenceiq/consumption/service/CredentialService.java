package com.sequenceiq.consumption.service;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.consumption.dto.Credential;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@Service
public class CredentialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialService.class);

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private SecretService secretService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public Credential getCredentialByEnvCrn(String envCrn) {
        try {
            CredentialResponse credentialResponse = credentialEndpoint.getByEnvironmentCrn(envCrn);
            SecretResponse secretResponse = credentialResponse.getAttributes();
            String attributes = secretService.getByResponse(secretResponse);
            return new Credential(
                    credentialResponse.getCloudPlatform(),
                    credentialResponse.getName(),
                    attributes,
                    credentialResponse.getCrn(),
                    credentialResponse.getAccountId());
        } catch (WebApplicationException e) {
            try (Response response = e.getResponse()) {
                if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                    LOGGER.error("Credential not found by environment CRN: {}", envCrn, e);
                    throw new BadRequestException(String.format("Credential not found by environment CRN: %s", envCrn), e);
                }
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                LOGGER.error("Failed to get credential for environment CRN [{}]: {}", envCrn, errorMessage);
                throw new CloudbreakServiceException(String.format("Failed to get credential: %s", errorMessage), e);
            }
        }
    }
}
