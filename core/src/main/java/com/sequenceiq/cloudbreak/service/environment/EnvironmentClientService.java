package com.sequenceiq.cloudbreak.service.environment;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

@Service
public class EnvironmentClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentClientService.class);

    @Inject
    private EnvironmentEndpoint environmentEndpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DetailedEnvironmentResponse getByName(String name) {
        try {
            return environmentEndpoint.getByName(name);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET Environment by name: %s, due to: %s. %s.", name, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Environment by name: %s, due to: '%s' ", name, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public DetailedEnvironmentResponse getByCrn(String crn) {
        try {
            return environmentEndpoint.getByCrn(crn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET Environment by crn: %s, due to: %s. %s.", crn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Environment by crn: %s, due to: '%s' ", crn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public SimpleEnvironmentResponses list() {
        try {
            return environmentEndpoint.list();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to LIST Environment due to: %s. %s.", e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to LIST Environment due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    public String getCrnByName(String environmentName) {
        try {
            return environmentEndpoint.getCrnByName(environmentName).getEnvironmentCrn();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET environmentCrn by name due to: %s. %s.", e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET environmentCrn by name due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
