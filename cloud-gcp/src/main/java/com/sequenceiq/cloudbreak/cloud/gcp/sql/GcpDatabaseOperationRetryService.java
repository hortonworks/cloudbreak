package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.SQLAdminRequest;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationsListResponse;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpExceptionUtil;

/**
 * Submits Cloud SQL instance operations and retries them while GCP rejects them with HTTP 409 because another operation
 * (e.g. an automated backup) holds the instance. Only the submission is retried, waiting for the accepted operation is
 * up to the caller. Conflicts are turned into {@link GcpDatabaseOperationConflictException} and only that type is retried,
 * any other error is rethrown unchanged without a retry. Kept as a separate bean so the retry proxy applies to calls from
 * the database server services. The defaults (12 attempts, 10s backoff doubling up to 30s) wait about 5 minutes in total
 * before giving up. Every conflict is logged as a single WARN line, which also lists the unfinished operations of the
 * instance on the first and on the last attempt.
 */
@Component
public class GcpDatabaseOperationRetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseOperationRetryService.class);

    private static final String MAX_ATTEMPTS_EXPRESSION = "${cb.gcp.database.conflict.retry.attemptCount:12}";

    private static final String OPERATION_DONE = "DONE";

    private static final long MAX_LISTED_OPERATIONS = 20L;

    @Value(MAX_ATTEMPTS_EXPRESSION)
    private int maxAttempts;

    @Retryable(retryFor = GcpDatabaseOperationConflictException.class, maxAttemptsExpression = MAX_ATTEMPTS_EXPRESSION,
            backoff = @Backoff(delayExpression = "${cb.gcp.database.conflict.retry.delayMs:10000}", multiplier = 2,
                    maxDelayExpression = "${cb.gcp.database.conflict.retry.maxDelayMs:30000}"))
    public Operation executeRetryingOnConflict(SQLAdminRequest<Operation> request, SQLAdmin sqlAdmin, String projectId, String instanceName)
            throws IOException {
        int attempt = currentAttempt();
        try {
            Operation operation = request.execute();
            if (attempt > 1) {
                LOGGER.info("Cloud SQL accepted the operation on instance '{}' at attempt {} after earlier conflicts", instanceName, attempt);
            }
            return operation;
        } catch (GoogleJsonResponseException e) {
            if (GcpExceptionUtil.conflictException(e)) {
                logConflict(sqlAdmin, projectId, instanceName, attempt, e);
                throw new GcpDatabaseOperationConflictException(e);
            }
            throw e;
        }
    }

    private int currentAttempt() {
        RetryContext retryContext = RetrySynchronizationManager.getContext();
        return retryContext == null ? 1 : retryContext.getRetryCount() + 1;
    }

    private void logConflict(SQLAdmin sqlAdmin, String projectId, String instanceName, int attempt, GoogleJsonResponseException e) {
        if (attempt == 1 || attempt >= maxAttempts) {
            LOGGER.warn("Cloud SQL rejected the operation on instance '{}' at attempt {}/{} because another operation is in progress: {}. "
                            + "Unfinished operations: {}",
                    instanceName, attempt, maxAttempts, e.getDetails(), describeUnfinishedOperations(sqlAdmin, projectId, instanceName));
        } else {
            LOGGER.warn("Cloud SQL rejected the operation on instance '{}' at attempt {}/{} because another operation is in progress: {}",
                    instanceName, attempt, maxAttempts, e.getDetails());
        }
    }

    private String describeUnfinishedOperations(SQLAdmin sqlAdmin, String projectId, String instanceName) {
        try {
            OperationsListResponse response = sqlAdmin.operations().list(projectId)
                    .setInstance(instanceName)
                    .setMaxResults(MAX_LISTED_OPERATIONS)
                    .execute();
            List<String> unfinishedOperations = Optional.ofNullable(response.getItems()).orElse(List.of()).stream()
                    .filter(operation -> !OPERATION_DONE.equals(operation.getStatus()))
                    .map(operation -> String.format("[name: %s, type: %s, status: %s, startTime: %s]",
                            operation.getName(), operation.getOperationType(), operation.getStatus(), operation.getStartTime()))
                    .toList();
            return unfinishedOperations.toString();
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("Could not list the operations of Cloud SQL instance '{}'", instanceName, e);
            return "could not be listed: " + e.getMessage();
        }
    }
}
