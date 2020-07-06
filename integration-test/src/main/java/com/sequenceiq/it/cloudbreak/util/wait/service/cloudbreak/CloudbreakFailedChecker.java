package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class CloudbreakFailedChecker<T extends CloudbreakWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakFailedChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, Status> desiredStatuses = waitObject.getDesiredStatuses();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (waitObject.isDeleted(actualStatuses)) {
                Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                        .getClusterStatusReason());
                LOGGER.error("Cluster '{}' has been terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' has been terminated. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (desiredStatuses.equals(actualStatuses)) {
                return true;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No cluster found with name '{}'", name, e);
        } catch (Exception e) {
            LOGGER.error("Failed to get cluster status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get cluster status: %s", e.getMessage()));
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            Map<String, Status> actualStatuses = Map.of("status", stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
            Map<String, String> actualStatusReasons = Map.of("stackStatusReason", stackStatus.getStatusReason(), "clusterStatusReason", stackStatus
                    .getClusterStatusReason());
            throw new TestFailException(String.format("Wait operation timed out, '%s' cluster has not been failed. Cluster status: '%s' " +
                    "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, failed to get cluster status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out, failed to get cluster status: %s",
                    e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' cluster is in the desired state '%s'", waitObject.getName(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), name);
            if (stackStatus == null) {
                LOGGER.info("'{}' cluster was not found. Exit waiting!", name);
                return true;
            }
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
        StackStatusV4Response stackStatus = waitObject.getStackEndpoint().getStatusByName(waitObject.getWorkspaceId(), waitObject.getName());
        return Map.of("status", stackStatus.getStatus().name(), "clusterStatus", stackStatus.getClusterStatus().name());
    }
}
