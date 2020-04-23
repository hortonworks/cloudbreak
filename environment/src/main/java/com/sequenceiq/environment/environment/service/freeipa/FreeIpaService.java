package com.sequenceiq.environment.environment.service.freeipa;

import java.util.Optional;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;

@Service
public class FreeIpaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaService.class);

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    public FreeIpaService(FreeIpaV1Endpoint freeIpaV1Endpoint) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
    }

    public DescribeFreeIpaResponse create(CreateFreeIpaRequest createFreeIpaRequest) {
        try {
            return freeIpaV1Endpoint.create(createFreeIpaRequest);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to create FreeIpa cluster for environment '%s' due to: '%s'",
                    createFreeIpaRequest.getEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public Optional<DescribeFreeIpaResponse> describe(String envCrn) {
        try {
            return Optional.of(freeIpaV1Endpoint.describe(envCrn));
        } catch (NotFoundException e) {
            LOGGER.warn("Could not find freeipa with envCrn: " + envCrn);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to describe FreeIpa cluster for environment '%s' due to: '%s'.", envCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void delete(String environmentCrn) {
        try {
            freeIpaV1Endpoint.delete(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to delete FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void attachChildEnvironment(AttachChildEnvironmentRequest attachChildEnvironmentRequest) {
        try {
            freeIpaV1Endpoint.attachChildEnvironment(attachChildEnvironmentRequest);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to attach child environment '%s' for '%s' due to: '%s'",
                    attachChildEnvironmentRequest.getChildEnvironmentCrn(), attachChildEnvironmentRequest.getParentEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    public void detachChildEnvironment(DetachChildEnvironmentRequest detachChildEnvironmentRequest) {
        try {
            freeIpaV1Endpoint.detachChildEnvironment(detachChildEnvironmentRequest);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to detach child environment '%s' due to: '%s'",
                    detachChildEnvironmentRequest.getChildEnvironmentCrn(), errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    void startFreeIpa(String environmentCrn) {
        try {
            freeIpaV1Endpoint.start(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to start FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }

    void stopFreeIpa(String environmentCrn) {
        try {
            freeIpaV1Endpoint.stop(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = e.getMessage();
            LOGGER.error(String.format("Failed to stop FreeIpa cluster for environment '%s' due to: '%s'", environmentCrn, errorMessage), e);
            throw new FreeIpaOperationFailedException(errorMessage, e);
        }
    }
}
