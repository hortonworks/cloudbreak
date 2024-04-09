package com.sequenceiq.environment.environment.service.externalizedcompute;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ExternalizedComputeClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClientService.class);

    @Inject
    private ExternalizedComputeClusterEndpoint endpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public FlowIdentifier createComputeCluster(ExternalizedComputeClusterRequest request) {
        return handleException(() -> endpoint.create(request), "Failed to create compute cluster");
    }

    public FlowIdentifier deleteComputeCluster(String environmentCrn, String name) {
        return handleException(() -> endpoint.delete(environmentCrn, name), "Failed to delete compute cluster");
    }

    public Optional<ExternalizedComputeClusterResponse> getComputeCluster(String environmentCrn, String name) {
        return handleException(() -> {
            try {
                return Optional.of(endpoint.describe(environmentCrn, name));
            } catch (NotFoundException e) {
                LOGGER.info("Could not find compute cluster: {}", name);
                return Optional.empty();
            }
        }, "Failed to describe compute cluster");
    }

    public List<ExternalizedComputeClusterResponse> list(String environmentCrn) {
        return handleException(() -> endpoint.list(environmentCrn), "Failed to list compute clusters");
    }

    private <T> T handleException(Supplier<T> function, String messageTemplate) {
        try {
            return function.get();
        } catch (WebApplicationException e) {
            String errorReason = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format(messageTemplate + " due to: %s", errorReason);
            LOGGER.warn(message, e);
            throw new ExternalizedComputeOperationFailedException(message, e);
        } catch (Exception e) {
            String message = String.format(messageTemplate + " due to: %s", e.getMessage());
            LOGGER.warn(message, e);
            throw new ExternalizedComputeOperationFailedException(message, e);
        }
    }
}
