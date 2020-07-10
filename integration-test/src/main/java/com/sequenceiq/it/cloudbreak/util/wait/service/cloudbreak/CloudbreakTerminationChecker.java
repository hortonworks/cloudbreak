package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class CloudbreakTerminationChecker<T extends CloudbreakWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, Status> desiredStatuses = waitObject.getDesiredStatuses();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (isDeleteFailed(actualStatuses)) {
                Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                        .getClusterStatusReason());
                LOGGER.error("Cluster '{}' termination failed (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (isNotDeleted(actualStatuses)) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No cluster found with name '{}'", name, e);
        } catch (Exception e) {
            LOGGER.error("Cluster termination failed, because of: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Cluster termination failed, because of: %s", e.getMessage()));
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                    .getClusterStatusReason());
            throw new TestFailException(String.format("Wait operation timed out! '%s' cluster termination failed. Cluster status: '%s' " +
                    "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out! Failed to get cluster status or statusReason:: %s", e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' cluster termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            if (isDeleteFailed(actualStatuses)) {
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
        String name = waitObject.getName();
        Long workspaceId = waitObject.getWorkspaceId();
        try {
            return Map.of("status", waitObject.getStackEndpoint().get(workspaceId, name,
                    Collections.emptySet()).getStatus().name(), "clusterStatus", waitObject.getDistroxEndpoint()
                    .getByName(name, Collections.emptySet()).getStatus().name());
        } catch (NotFoundException e) {
            LOGGER.warn("No cluster found with name '{}'! It has been deleted successfully.", name, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }

    private boolean isDeleteFailed(Map<String, Status> clusterStatuses) {
        List<Status> actualStatuses = new ArrayList<>(clusterStatuses.values());
        return actualStatuses.contains(DELETE_FAILED);
    }

    private boolean isNotDeleted(Map<String, Status> clusterStatuses) {
        Map<String, Status> deletedStatuses = Map.of("status", DELETE_COMPLETED, "clusterStatus", DELETE_COMPLETED);
        return !deletedStatuses.equals(clusterStatuses);
    }

}
