package com.sequenceiq.cloudbreak.service.environment.credential;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@Service
public class CredentialClientService implements AuthorizationResourceCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialClientService.class);

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    public Credential getByName(String name) {
        try {
            CredentialResponse credentialResponse = credentialEndpoint.getByName(name);
            return credentialConverter.convert(credentialResponse);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Credential with name: %s, due to: '%s' ", name, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public Credential getByCrn(String crn) {
        try {
            CredentialResponse credentialResponse = credentialEndpoint.getByResourceCrn(crn);
            return credentialConverter.convert(credentialResponse);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Credential by crn: %s, due to: '%s' ", crn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public Credential getByEnvironmentCrn(String envCrn) {
        try {
            //TODO CloudPlatfrom needs to be part of the response
            //TODO Revise paramaters because most of them should be a secret
            CredentialResponse credentialResponse = ThreadBasedUserCrnProvider
                    .doAsInternalActor(
                            () -> credentialEndpoint.getByEnvironmentCrn(envCrn));
            return credentialConverter.convert(credentialResponse);
        } catch (WebApplicationException | IllegalStateException e) {
            String message = String.format("Failed to GET Credential by environment crn: %s, due to: '%s' ", envCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public CloudCredential getCloudCredential(String environmentCrn) {
        return credentialToCloudCredentialConverter.convert(getByEnvironmentCrn(environmentCrn));
    }

    public ExtendedCloudCredential getExtendedCloudCredential(String environmentCrn) {
        return credentialToExtendedCloudCredentialConverter.convert(getByEnvironmentCrn(environmentCrn));
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        // is there a better way to get credential crn by name in core?
        return getByName(resourceName).getCrn();
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CREDENTIAL;
    }
}
