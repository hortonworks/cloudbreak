package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.AuthorizationResourceCrnProvider;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

@Service
public class EnvironmentClientService implements EnvironmentPropertyProvider, AuthorizationResourceCrnProvider {

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

    public DetailedEnvironmentResponse getByCrnAsInternal(String environmentCrn) {
        DetailedEnvironmentResponse environment = null;
        if (Objects.nonNull(environmentCrn)) {
            environment = measure(() -> ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> getByCrn(environmentCrn)),
                    LOGGER,
                    "Get Environment from Environment service in as internal user took {} ms");
        }
        return environment;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getCrnByName(resourceName);
    }

}
