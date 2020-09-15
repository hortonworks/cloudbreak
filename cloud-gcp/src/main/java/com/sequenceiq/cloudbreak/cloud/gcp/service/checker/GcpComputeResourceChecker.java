package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.Location;

@Component
public class GcpComputeResourceChecker {

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Operation check(GcpContext context,  String operationId) throws IOException {
        if (operationId == null) {
            return null;
        }
        try {
            Operation execute = GcpStackUtil.globalOperations(context.getCompute(), context.getProjectId(), operationId).execute();
            checkComputeOperationError(execute);
            return execute;
        } catch (GoogleJsonResponseException e) {
            return handleException(context, operationId, e);
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

    protected Operation handleException(GcpContext context, String operationId, GoogleJsonResponseException e) throws IOException {
        if (e.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
            Location location = context.getLocation();
            try {
                Operation execute = GcpStackUtil.regionOperations(context.getCompute(), context.getProjectId(), operationId, location.getRegion()).execute();
                checkComputeOperationError(execute);
                return execute;
            } catch (GoogleJsonResponseException e1) {
                if (e1.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e1.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
                    Operation execute = GcpStackUtil.zoneOperations(context.getCompute(), context.getProjectId(), operationId,
                            location.getAvailabilityZone()).execute();
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
