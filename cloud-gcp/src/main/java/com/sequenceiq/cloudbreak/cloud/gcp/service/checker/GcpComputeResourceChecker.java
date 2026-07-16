package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Component
public class GcpComputeResourceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpComputeResourceChecker.class);

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Operation check(GcpContext context, OperationInfo operationInfo, Iterable<CloudResource> resources) throws IOException {
        if (operationInfo != null && operationInfo.operationId() != null) {
            String operationId = operationInfo.operationId();
            return switch (operationInfo.operationType()) {
                case GLOBAL -> getAndCheckOperation(context, operationId, this::getGlobalOperation);
                case REGIONAL -> getAndCheckOperation(context, operationId, this::getRegionOperation);
                case ZONAL -> getAndCheckOperation(context, operationId, (ctx, opId) -> getZoneOperation(ctx, opId, resources));
                case UNKNOWN -> getOperationWithFallback(context, operationId, resources);
                case null -> getOperationWithFallback(context, operationId, resources);
            };
        } else {
            return null;
        }
    }

    private Operation getOperationWithFallback(GcpContext context, String operationId, Iterable<CloudResource> resources) throws IOException {
        List<OperationGetter> operationGetters = List.of(
                new OperationGetter(OperationType.GLOBAL, this::getGlobalOperation),
                new OperationGetter(OperationType.REGIONAL, this::getRegionOperation),
                new OperationGetter(OperationType.ZONAL, (ctx, opId) -> getZoneOperation(ctx, opId, resources)));
        Iterator<OperationGetter> iterator = operationGetters.iterator();
        while (iterator.hasNext()) {
            OperationGetter operationGetter = iterator.next();
            try {
                LOGGER.info("Getting {} operation with id: {}", operationGetter.operationType(), operationId);
                return getAndCheckOperation(context, operationId, operationGetter.operationGetterFunction());
            } catch (GoogleJsonResponseException googleException) {
                if (!iterator.hasNext() || !isFallbackNeeded(googleException)) {
                    LOGGER.error("Exception during {} operation checking, stopping fallback chain.", operationGetter.operationType(), googleException);
                    throw googleException;
                } else {
                    LOGGER.warn("Operation with {} id not found with {} type, try to fallback", operationId, operationGetter.operationType());
                }
            }
        }
        throw new CloudConnectorException("All operation fallback strategies failed unexpectedly.");
    }

    private void checkComputeOperationError(Operation execute) {
        if (execute.getError() != null) {
            String msg = null;
            StringBuilder error = new StringBuilder();
            if (execute.getError().getErrors() != null) {
                for (Operation.Error.Errors errors : execute.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            }
            LOGGER.warn("Error during operation(id: {}) checking: {}", execute.getName(), msg);
            throw new CloudConnectorException(msg);
        }
    }

    private boolean isFallbackNeeded(GoogleJsonResponseException googleException) {
        if (googleException.getDetails() != null) {
            Object code = googleException.getDetails().get("code");
            return code != null && (code.equals(HttpStatus.SC_NOT_FOUND) || code.equals(HttpStatus.SC_FORBIDDEN));
        } else {
            return false;
        }
    }

    private Operation getAndCheckOperation(GcpContext context, String operationId, OperationGetterFunction operationGetterFunction) throws IOException {
        try {
            Operation operation = operationGetterFunction.get(context, operationId);
            checkComputeOperationError(operation);
            return operation;
        } catch (InterruptedIOException interruptedIOException) {
            String message = String.format("Failed to check the '%s' operation on the compute for '%s' due to network issues.", operationId,
                    context.getName());
            LOGGER.warn(message, interruptedIOException);
            throw new CloudConnectorException(message, interruptedIOException);
        }
    }

    private Operation getGlobalOperation(GcpContext context, String operationId) throws IOException {
        LOGGER.info("Getting global operation with id: {}", operationId);
        return gcpStackUtil.globalOperations(context.getCompute(), context.getProjectId(), operationId).execute();
    }

    private Operation getRegionOperation(GcpContext context, String operationId) throws IOException {
        Location location = context.getLocation();
        Region region = location.getRegion();
        LOGGER.info("Getting region operation with id: {}, region: {}", operationId, region);
        return gcpStackUtil.regionOperations(context.getCompute(), context.getProjectId(), operationId, region).execute();
    }

    private Operation getZoneOperation(GcpContext context, String operationId, Iterable<CloudResource> resources) throws IOException {
        Location location = context.getLocation();
        CloudResource cloudResource = resources.iterator().next();
        String availabilityZone = Strings.isNullOrEmpty(cloudResource.getAvailabilityZone())
                ? location.getAvailabilityZone().value() : cloudResource.getAvailabilityZone();
        LOGGER.info("Getting zone operation with id: {}, zone: {}", operationId, availabilityZone);
        return gcpStackUtil.zoneOperation(context.getCompute(), context.getProjectId(), operationId, availabilityZone).execute();
    }

    @FunctionalInterface
    private interface OperationGetterFunction {
        Operation get(GcpContext context, String operationId) throws IOException;
    }

    private record OperationGetter(OperationType operationType, OperationGetterFunction operationGetterFunction) {
    }
}
