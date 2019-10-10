package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder.OPERATION_ID;

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
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@Component
public class GcpResourceChecker {

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Operation check(GcpContext context, DynamicModel resource) throws IOException {
        String operation = resource.getStringParameter(OPERATION_ID);
        if (operation == null) {
            return null;
        }
        try {
            Operation execute = GcpStackUtil.globalOperations(context.getCompute(), context.getProjectId(), operation).execute();
            checkError(execute);
            return execute;
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
                Location location = context.getLocation();
                try {
                    Operation execute = GcpStackUtil.regionOperations(context.getCompute(), context.getProjectId(), operation, location.getRegion()).execute();
                    checkError(execute);
                    return execute;
                } catch (GoogleJsonResponseException e1) {
                    if (e1.getDetails().get("code").equals(HttpStatus.SC_NOT_FOUND) || e1.getDetails().get("code").equals(HttpStatus.SC_FORBIDDEN)) {
                        Operation execute = GcpStackUtil.zoneOperations(context.getCompute(), context.getProjectId(), operation,
                                location.getAvailabilityZone()).execute();
                        checkError(execute);
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

    private void checkError(Operation execute) {
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
}
