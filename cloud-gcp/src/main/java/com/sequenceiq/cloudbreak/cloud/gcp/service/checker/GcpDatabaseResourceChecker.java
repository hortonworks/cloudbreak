package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.OperationError;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;

@Component
public class GcpDatabaseResourceChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseResourceChecker.class);

    @Retryable(value = CloudConnectorException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public com.google.api.services.sqladmin.model.Operation check(SQLAdmin sqlAdmin, AuthenticatedContext ac, String operationId) throws IOException {
        if (operationId == null) {
            return null;
        }
        try {
            String projectId = getProjectId(ac);
            com.google.api.services.sqladmin.model.Operation execute = getSqlAdminOperations(sqlAdmin, operationId, projectId).execute();
            checkSqlOperationError(execute);
            return execute;
        } catch (InterruptedIOException interruptedIOException) {
            String message = String.format("Failed to check the '%s' operation on the database for '%s' due to network issues.", operationId,
                    ac.getCloudContext().getName());
            LOGGER.warn(message, interruptedIOException);
            throw new CloudConnectorException(message, interruptedIOException);
        } catch (Exception e) {
            String message = String.format("Failed to check the '%s' operation on the database for '%s'.", operationId,
                    ac.getCloudContext().getName());
            LOGGER.warn(message, e);
            throw e;
        }
    }

    protected void checkSqlOperationError(com.google.api.services.sqladmin.model.Operation execute) {
        if (execute.getError() != null) {
            String msg = null;
            StringBuilder error = new StringBuilder();
            if (execute.getError().getErrors() != null) {
                LOGGER.info("Gcp database operation response contains errors.");
                for (OperationError operationError : execute.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s",
                            operationError.getCode(), operationError.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
                LOGGER.warn("Gcp database operation failed and return with the following errors: '{}'", msg);
            }
            throw new CloudConnectorException(msg);
        }
    }

    @VisibleForTesting
    SQLAdmin.Operations.Get getSqlAdminOperations(SQLAdmin sqlAdmin, String operationId, String projectId) throws IOException {
        return GcpStackUtil.sqlAdminOperations(sqlAdmin, projectId, operationId);
    }

    @VisibleForTesting
    String getProjectId(AuthenticatedContext ac) {
        return GcpStackUtil.getProjectId(ac.getCloudCredential());
    }
}
