package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class EnvironmentOperationChecker<T extends EnvironmentWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        EnvironmentStatus desiredStatus = waitObject.getDesiredStatus();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            if (environment == null) {
                throw new TestFailException(String.format("'%s' environment was not found.", crn));
            }
            String name = environment.getName();
            EnvironmentStatus status = environment.getEnvironmentStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' environment. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (isDeletionInProgress(status) || status.equals(ARCHIVED)) {
                LOGGER.error("Environment '{}' '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Environment '%s' '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name, crn,
                        status));
            }
            if (status.isFailed()) {
                LOGGER.error("Environment '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Environment '%s' '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, crn, status, environment.getStatusReason()));
            }
            if (desiredStatus.equals(status)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Environment has been failed. Also failed to get environment status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Environment has been failed. Also failed to get environment status: ", e.getMessage()));
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            if (environment == null) {
                throw new TestFailException(String.format("'%s' environment was not found.", crn));
            }
            String name = environment.getName();
            EnvironmentStatus status = environment.getEnvironmentStatus();
            throw new TestFailException(String.format("Wait operation timed out, environment '%s' '%s' has been failed. Environment status: '%s' "
                    + "statusReason: '%s'", name, crn, status, environment.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, environment has been failed. Also failed to get environment status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out, environment has been failed. Also failed to get environment status: ",
                    e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' environment is in the desired state '%s'",
                waitObject.getCrn(), waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            if (environment == null) {
                LOGGER.info("'{}' environment was not found. Exit waiting!", crn);
                return true;
            }
            EnvironmentStatus status = environment.getEnvironmentStatus();
            if (status.equals(CREATE_FAILED)) {
                return true;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get environment due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get environment, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return Map.of("status", waitObject.getEndpoint().getByCrn(waitObject.getCrn()).getEnvironmentStatus().name());
    }

    private boolean isDeletionInProgress(EnvironmentStatus environmentStatus) {
        Set<EnvironmentStatus> deleteInProgressStatuses = Set.of(DELETE_INITIATED, NETWORK_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS,
                FREEIPA_DELETE_IN_PROGRESS, CLUSTER_DEFINITION_CLEANUP_PROGRESS, UMS_RESOURCE_DELETE_IN_PROGRESS, IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
                S3GUARD_TABLE_DELETE_IN_PROGRESS, DATAHUB_CLUSTERS_DELETE_IN_PROGRESS, DATALAKE_CLUSTERS_DELETE_IN_PROGRESS, PUBLICKEY_DELETE_IN_PROGRESS);
        return deleteInProgressStatuses.contains(environmentStatus);
    }
}
