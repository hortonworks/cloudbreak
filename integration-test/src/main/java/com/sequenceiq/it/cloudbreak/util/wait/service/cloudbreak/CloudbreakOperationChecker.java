package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class CloudbreakOperationChecker<T extends CloudbreakWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, Status> desiredStatuses = waitObject.getDesiredStatuses();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name, waitObject.getAccountId());
            if (stackStatus == null) {
                throw new TestFailException(String.format("'%s' stack was not found.", name));
            }
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (isDeletionInProgress(actualStatuses) || waitObject.isDeleted(actualStatuses)) {
                LOGGER.error("Cluster '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name,
                        actualStatuses));
            }
            if (waitObject.isFailed(actualStatuses)) {
                Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                        .getClusterStatusReason());
                LOGGER.error("Cluster '{}' is in failed state (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (desiredStatuses.equals(actualStatuses)) {
                LOGGER.info("Cluster '{}' is in desired state (status:'{}').", name, actualStatuses);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get cluster status or statusReason: %s", e.getMessage()));
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name, waitObject.getAccountId());
            if (stackStatus == null) {
                throw new TestFailException(String.format("'%s' cluster was not found.", name));
            }
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                    .getClusterStatusReason());
            throw new TestFailException(String.format("Wait operation timed out! Cluster '%s' has been failed. Cluster status: '%s' "
                    + "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out! Failed to get cluster status or statusReason: %s", e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' cluster is in the desired state '%s'",
                waitObject.getName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name, waitObject.getAccountId());
            if (stackStatus == null) {
                LOGGER.info("'{}' cluster was not found. Exit waiting!", name);
                return true;
            }
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            if (isCreateFailed(actualStatuses)) {
                return true;
            }
            return waitObject.isFailed(actualStatuses);
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get cluster due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get cluster, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), waitObject.getName(),
                waitObject.getAccountId());
        return Map.of("status", stackStatus.getStatus().name(), "clusterStatus", stackStatus.getClusterStatus().name());
    }

    private boolean isDeletionInProgress(Map<String, Status> clusterStatuses) {
        Set<Status> deleteInProgressStatuses = Set.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return !Sets.intersection(Set.of(clusterStatuses.values()), deleteInProgressStatuses).isEmpty();
    }

    private boolean isCreateFailed(Map<String, Status> clusterStatuses) {
        List<Status> actualStatuses = new ArrayList<>(clusterStatuses.values());
        return actualStatuses.contains(CREATE_FAILED);
    }
}
