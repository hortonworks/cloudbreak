package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

import java.util.List;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class InstanceWaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFailedChecker.class);

    private final String name;

    private final String hostGroup;

    private final InstanceStatus desiredStatus;

    private final TestContext testContext;

    public InstanceWaitObject(TestContext testContext, String name, String hostGroup, InstanceStatus desiredStatus) {
        this.testContext = testContext;
        this.name = name;
        this.hostGroup = hostGroup;
        this.desiredStatus = desiredStatus;
    }

    public String getName() {
        return name;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public InstanceStatus getDesiredStatus() {
        return desiredStatus;
    }

    public List<InstanceGroupV4Response> getInstanceGroups() {
        try {
            return testContext.getMicroserviceClient(CloudbreakClient.class)
                    .getCloudbreakClient().distroXV1Endpoint().getByName(name, Set.of()).getInstanceGroups();
        } catch (NotFoundException e) {
            LOGGER.info("SDX '{}' instance groups are present for validation.", getName());
            return testContext.getSdxClient().getSdxClient().sdxEndpoint().getDetail(name, Set.of()).getStackV4Response().getInstanceGroups();
        } catch (Exception e) {
            LOGGER.error("Instance groups cannot be determined, because of: {}", e.getMessage(), e);
            throw new TestFailException("Instance groups cannot be determined", e);
        }
    }

    public boolean isFailed(InstanceStatus instanceStatus) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(instanceStatus);
    }

    public boolean isDeleted(InstanceStatus instanceStatus) {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(instanceStatus);
    }

    public boolean isDeleteFailed(InstanceStatus instanceStatus) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(instanceStatus);
    }

    public boolean isNotDeleted(InstanceStatus instanceStatus) {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return !deletedStatuses.contains(instanceStatus);
    }
}