package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;

import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceOperationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        String hostGroup = waitObject.getHostGroup();
        try {
            waitObject.fetchData();
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            LOGGER.info("Waiting for the '{}'. Actual instance state is: '{}'", desiredStatuses, instanceStatus);
            if (instanceStatus.equals(DELETE_REQUESTED) || waitObject.isDeleted()) {
                LOGGER.error("The '{}' instance group has been getting terminated (status:'{}'), waiting is cancelled.", instanceGroupName,
                        instanceStatus);
                throw new TestFailException(String.format("The '%s' instance group has been getting terminated, waiting is cancelled." +
                        " Status: '%s' statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
            }
            if (waitObject.isFailed()) {
                LOGGER.error("The '{}' instance group is in failed state (status:'{}'), waiting is cancelled.", instanceGroupName, instanceStatus);
                throw new TestFailException(String.format("The '%s' instance group is in failed state. Status: '%s' statusReason: '%s'",
                        instanceGroupName, instanceStatus, hostStatusReason));
            }
            if (waitObject.isInDesiredStatus()) {
                LOGGER.info("The '{}' instance group is in desired state (status:'{}').", instanceGroupName, instanceStatus);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get '{}' instance group status, because of {}", hostGroup, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get '%s' instance group status", hostGroup), e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            throw new TestFailException(String.format("Wait operation timed out, '%s' instance group has been failed. Instance status: '%s' " +
                    "statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, failed to get instance status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, failed to get instance status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.", waitObject.getHostGroup(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        try {
            InstanceStatus instanceStatus = waitObject.getInstanceStatus();
            if (instanceStatus.equals(ORCHESTRATION_FAILED)) {
                return true;
            }
            return waitObject.isFailed();

        } catch (ProcessingException e) {
            LOGGER.error("Exit waiting! Failed to get instance group due to API client exception: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }
}