package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class InstanceWaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFailedChecker.class);

    private final String name;

    private final Map<String, InstanceStatus> desiredStatuses;

    private final TestContext testContext;

    public InstanceWaitObject(TestContext testContext, String name, Map<String, InstanceStatus> desiredStatuses) {
        this.testContext = testContext;
        this.name = name;
        this.desiredStatuses = desiredStatuses;
    }

    public String getName() {
        return name;
    }

    public Map<String, InstanceStatus> getDesiredStatuses() {
        return desiredStatuses;
    }

    public List<InstanceGroupV4Response> getInstanceGroups() {
        try {
            return testContext.getCloudbreakClient().getCloudbreakClient().distroXV1Endpoint().getByName(name, Set.of()).getInstanceGroups();
        } catch (NotFoundException e) {
            LOGGER.info("SDX '{}' instance groups are present for validation.", getName());
            return testContext.getSdxClient().getSdxClient().sdxEndpoint().getDetail(name, Set.of()).getStackV4Response().getInstanceGroups();
        } catch (Exception e) {
            LOGGER.error("Instance groups cannot be determined, because of: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Instance groups cannot be determined, because of: %s", e.getMessage()));
        }
    }

    public boolean isFailed(InstanceStatus instanceStatus) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(instanceStatus);
    }

    public boolean isFailed(Map<String, InstanceStatus> instanceStatuses) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, SERVICES_UNHEALTHY, DECOMMISSION_FAILED);
        return !Sets.intersection(Set.of(instanceStatuses.values()), failedStatuses).isEmpty();
    }

    public boolean isDeleted(InstanceStatus instanceStatus) {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(instanceStatus);
    }
}
