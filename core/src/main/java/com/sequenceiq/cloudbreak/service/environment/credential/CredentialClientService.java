package com.sequenceiq.cloudbreak.service.environment.credential;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;

@Service
public class CredentialClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialClientService.class);

    @Inject
    private CredentialEndpoint credentialEndpoint;

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private CredentialConverter credentialConverter;

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
            CredentialResponse credentialResponse = credentialEndpoint.getByEnvironmentCrn(envCrn);
            return credentialConverter.convert(credentialResponse);
        } catch (WebApplicationException | IllegalStateException e) {
            String message = String.format("Failed to GET Credential by environment crn: %s, due to: '%s' ", envCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
