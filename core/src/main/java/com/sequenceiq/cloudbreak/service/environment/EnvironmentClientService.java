package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

@Service
public class EnvironmentClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    public DetailedEnvironmentResponse getByName(String name) {
        try {
            return environmentEndpoint.getByName(name);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Environment by name: %s, due to: '%s' ", name, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DetailedEnvironmentResponse getByCrn(String crn) {
        try {
            return environmentEndpoint.getByCrn(crn);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Environment by crn: %s, due to: '%s' ", crn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public SimpleEnvironmentResponses list() {
        try {
            return environmentEndpoint.list();
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to LIST Environment due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DetailedEnvironmentResponse create(@Valid EnvironmentRequest request) {
        try {
            return environmentEndpoint.post(request);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to CREATE Environment by name: %s, due to: '%s' ", request.getName(), e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DetailedEnvironmentResponse edit(String environmentCrn, @NotNull EnvironmentEditRequest request) {
        try {
            return environmentEndpoint.editByCrn(environmentCrn, request);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to EDIT Environment by crn: %s, due to: '%s' ", environmentCrn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public SimpleEnvironmentResponse delete(String name) {
        try {
            return environmentEndpoint.deleteByName(name);
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to DELETE Environment by name: %s, due to: '%s' ", name, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
