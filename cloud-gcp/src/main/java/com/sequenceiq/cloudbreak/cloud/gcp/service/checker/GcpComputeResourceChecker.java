package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.io.IOException;
import java.io.InterruptedIOException;

import javax.inject.Inject;

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
    public Operation check(GcpContext context,  String operationId, Iterable<CloudResource> resources) throws IOException {
        if (operationId == null) {
            return null;
        }
        try {
            Operation execute = gcpStackUtil.globalOperations(context.getCompute(), context.getProjectId(), operationId).execute();
            checkComputeOperationError(execute);
            return execute;
        } catch (InterruptedIOException interruptedIOException) {
            String message = String.format("Failed to check the '%s' operation on the database for '%s' due to network issues.", operationId,
                    context.getName());
            LOGGER.warn(message, interruptedIOException);
            throw new CloudConnectorException(message, interruptedIOException);
        } catch (GoogleJsonResponseException e) {
            String message = String.format("Failed to check the '%s' operation on the resource for '%s'.", operationId,
                    context.getName());
            LOGGER.warn(message, e);
            return handleException(context, operationId, resources, e);
        }
    }

    protected void checkComputeOperationError(Operation execute) {
        if (execute.getError() != null) {
            String msg = null;
            StringBuilder error = new StringBuilder();
            if (execute.getError().getErrors() != null) {
                for (Operation.Error.Errors errors : execute.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            }
            throw new CloudConnectorException(msg);
        }
    }

    protected Operation handleException(GcpContext context, String operationId, Iterable<CloudResource> resources,
        GoogleJsonResponseException e) throws IOException {
        if (e.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
            Location location = context.getLocation();
            Region region = location.getRegion();
            CloudResource cloudResource = resources.iterator().next();
            String availabilityZone = Strings.isNullOrEmpty(cloudResource.getAvailabilityZone())
                    ? location.getAvailabilityZone().value() : cloudResource.getAvailabilityZone();
            try {
                Operation execute = gcpStackUtil.regionOperations(context.getCompute(), context.getProjectId(), operationId, region).execute();
                checkComputeOperationError(execute);
                return execute;
            } catch (GoogleJsonResponseException e1) {
                if (e1.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e1.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
                    Operation execute = gcpStackUtil.zoneOperations(context.getCompute(), context.getProjectId(), operationId,
                            availabilityZone).execute();
                    checkComputeOperationError(execute);
                    return execute;
                } else {
                    throw e1;
                }
            }
        } else {
            throw e;
        }
    }
}
