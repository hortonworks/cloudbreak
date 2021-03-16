package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.Map;
import java.util.NoSuchElementException;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceTerminationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String hostGroup = waitObject.getHostGroup();
        try {
            waitObject.fetchData();
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            if (waitObject.isDeleteFailed()) {
                LOGGER.error("The '{}' instance group termination failed (status:'{}'), waiting is cancelled.", instanceGroupName, instanceStatus);
                throw new TestFailException(String.format("The '%s' instance group termination failed, waiting is cancelled. " +
                        "Status: '%s' statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
            }
            if (!waitObject.isDeleted()) {
                return false;
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("{} instance group is not present, may this was deleted.", hostGroup, e);
        } catch (Exception e) {
            LOGGER.error("'{}' instance group deletion has been failed, because of: {}", hostGroup, e.getMessage(), e);
            throw new TestFailException(String.format("'%s' instance group deletion has been failed", hostGroup), e);
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            throw new TestFailException(String.format("Wait operation timed out! '%s' instance group termination failed. Instance status: '%s' " +
                    "statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));

        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get instance status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out! Failed to get instance status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.'", waitObject.getHostGroup(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        try {
            if (waitObject.isDeleteFailed()) {
                return false;
            }
            return waitObject.isFailed();

        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get instance group due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.warn("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }
}