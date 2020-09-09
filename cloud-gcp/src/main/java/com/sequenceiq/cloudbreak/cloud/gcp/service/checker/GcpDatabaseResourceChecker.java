package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.io.IOException;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.OperationError;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;

@Component
public class GcpDatabaseResourceChecker {

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public com.google.api.services.sqladmin.model.Operation check(SQLAdmin sqlAdmin, AuthenticatedContext ac, String operationId) throws IOException {
        if (operationId == null) {
            return null;
        }
        try {
            String projectId = GcpStackUtil.getProjectId(ac.getCloudCredential());
            com.google.api.services.sqladmin.model.Operation execute = GcpStackUtil.sqlAdminOperations(sqlAdmin, projectId, operationId).execute();
            checkSqlOperationError(execute);
            return execute;
        } catch (GoogleJsonResponseException e) {
            throw e;
        }
    }

    protected void checkSqlOperationError(com.google.api.services.sqladmin.model.Operation execute) {
        if (execute.getError() != null) {
            String msg = null;
            StringBuilder error = new StringBuilder();
            if (execute.getError().getErrors() != null) {
                for (OperationError operationError : execute.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s",
                            operationError.getCode(), operationError.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            }
            throw new CloudConnectorException(msg);
        }
    }
}
