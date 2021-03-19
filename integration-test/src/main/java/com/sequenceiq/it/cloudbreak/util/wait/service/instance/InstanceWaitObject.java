package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class InstanceWaitObject implements WaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFailedChecker.class);

    private final String name;

    private final String hostGroup;

    private final InstanceStatus desiredStatus;

    private final TestContext testContext;

    private List<InstanceGroupV4Response> instanceGroups;

    public InstanceWaitObject(TestContext testContext, String name, String hostGroup, InstanceStatus desiredStatus) {
        this.testContext = testContext;
        this.name = name;
        this.hostGroup = hostGroup;
        this.desiredStatus = desiredStatus;
    }

    @Override
    public void fetchData() {
        try {
            instanceGroups = testContext.getMicroserviceClient(CloudbreakClient.class)
                    .getDefaultClient().distroXV1Endpoint().getByName(name, Set.of()).getInstanceGroups();
        } catch (NotFoundException e) {
            LOGGER.info("SDX '{}' instance groups are present for validation.", getName());
            instanceGroups = testContext.getSdxClient().getDefaultClient().sdxEndpoint().getDetail(name, Set.of()).getStackV4Response().getInstanceGroups();
        } catch (Exception e) {
            LOGGER.error("Instance groups cannot be determined, because of: {}", e.getMessage(), e);
            throw new TestFailException("Instance groups cannot be determined", e);
        }
    }

    @Override
    public boolean isDeleteFailed() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(getInstanceStatus());
    }

    @Override
    public Map<String, String> actualStatuses() {
        Optional<InstanceGroupV4Response> instanceGroup = getInstanceGroup();
        if (instanceGroup.isPresent()) {
            Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                    .get().getMetadata().stream().findFirst();
            if (instanceMetaData.isPresent()) {
                InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                return Map.of(STATUS, instanceMetaDataV4Response.getInstanceStatus().name());
            } else {
                LOGGER.error("'{}' instance metadata is empty, may instance group was deleted. ", hostGroup);
                return Map.of(STATUS, DELETED_ON_PROVIDER_SIDE.name());
            }
        } else {
            LOGGER.error("'{}' instance group is not present, may this was deleted. ", hostGroup);
            return Map.of(STATUS, DELETED_ON_PROVIDER_SIDE.name());
        }
    }

    @Override
    public Map<String, String> actualStatusReason() {
        String statusReason = getInstanceMetadata().getStatusReason();
        if (statusReason != null) {
            return Map.of(STATUS_REASON, statusReason);
        }
        return Map.of();
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return Map.of(STATUS, desiredStatus.name());
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isDeleted() {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(getInstanceStatus());
    }

    @Override
    public boolean isFailed() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(getInstanceStatus());
    }

    @Override
    public boolean isDeletionInProgress() {
        return false;
    }

    @Override
    public boolean isCreateFailed() {
        return false;
    }

    @Override
    public boolean isDeletionCheck() {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(desiredStatus);
    }

    @Override
    public boolean isFailedCheck() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(desiredStatus);
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public InstanceStatus getInstanceStatus() {
        return getInstanceMetadata().getInstanceStatus();
    }

    public InstanceMetaDataV4Response getInstanceMetadata() {
        Optional<InstanceGroupV4Response> instanceGroup = getInstanceGroup();
        if (instanceGroup.isPresent()) {
            Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                    .get().getMetadata().stream().findFirst();
            if (instanceMetaData.isPresent()) {
                return instanceMetaData.get();
            } else {
                LOGGER.error("'{}' instance group metadata is empty.", hostGroup);
                throw new TestFailException(String.format("'%s' instance group metadata is empty.", hostGroup));
            }
        } else {
            LOGGER.error("'{}' instance group is not present.", hostGroup);
            throw new TestFailException(String.format("'%s' instance group is not present.", hostGroup));
        }
    }

    public Optional<InstanceGroupV4Response> getInstanceGroup() {
        return instanceGroups
                .stream()
                .filter(ig -> ig.getName().equals(hostGroup))
                .findFirst();
    }
}