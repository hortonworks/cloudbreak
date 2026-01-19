package com.sequenceiq.it.cloudbreak.util.wait.service.instance.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.TERMINATED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;

public class FreeIpaInstanceWaitObject implements InstanceWaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceWaitObject.class);

    private final String environmentCrn;

    private final List<String> instanceIds;

    private final InstanceStatus desiredStatus;

    private final TestContext testContext;

    private List<InstanceGroupResponse> instanceGroups;

    public FreeIpaInstanceWaitObject(TestContext testContext, String environmentCrn, List<String> instanceIds, InstanceStatus desiredStatus) {
        this.testContext = testContext;
        this.environmentCrn = environmentCrn;
        this.instanceIds = instanceIds;
        this.desiredStatus = desiredStatus;
    }

    @Override
    public void fetchData() {
        try {
            instanceGroups = testContext.getMicroserviceClient(FreeIpaClient.class)
                    .getDefaultClient(testContext).getFreeIpaV1Endpoint().describe(environmentCrn).getInstanceGroups();
        } catch (Exception e) {
            LOGGER.error("FreeIpa instance groups cannot be determined, because of: {}", e.getMessage(), e);
            throw new TestFailException("FreeIpa instance groups cannot be determined", e);
        }
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (instanceGroups == null) {
            return Collections.emptyMap();
        }
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId,
                        instanceMetaData -> instanceMetaData.getInstanceStatus().name()));
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId,
                        instanceMetaData -> {
                            String statusReason = instanceMetaData.getState();
                            if (StringUtils.isBlank(statusReason)) {
                                return "Status reason is NOT available for FreeIPA instance!";
                            } else {
                                return statusReason;
                            }
                        }
                ));
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return instanceIds.stream().collect(Collectors.toMap(instanceId -> instanceId, instanceId -> desiredStatus.name()));
    }

    @Override
    public String getName() {
        return environmentCrn;
    }

    @Override
    public boolean isDeleted() {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.containsAll(getInstanceStatuses().values());
    }

    @Override
    public boolean isFailedButIgnored() {
        return false;
    }

    @Override
    public boolean isDeleteFailed() {
        return getInstanceStatuses().values().stream().anyMatch(FAILED::equals);
    }

    @Override
    public boolean isFailed() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED);
        return getInstanceStatuses().values().stream().anyMatch(failedStatuses::contains);
    }

    @Override
    public boolean isDeletionInProgress() {
        return getInstanceStatuses().values().stream().allMatch(DELETE_REQUESTED::equals);
    }

    @Override
    public boolean isCreateFailed() {
        return getInstanceStatuses().values().stream().anyMatch(FAILED::equals);
    }

    @Override
    public boolean isDeletionCheck() {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(desiredStatus);
    }

    @Override
    public boolean isFailedCheck() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED);
        return failedStatuses.contains(desiredStatus);
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public List<String> getFetchedInstanceIds() {
        return instanceGroups
                .stream()
                .flatMap(instanceGroup -> instanceGroup.getMetaData().stream())
                .map(InstanceMetaDataResponse::getInstanceId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getFetchedInstanceStatuses() {
        return getInstanceMetaDatas().stream().collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId,
                instanceMetaData -> instanceMetaData.getInstanceStatus().name()));
    }

    public Map<String, InstanceStatus> getInstanceStatuses() {
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataResponse::getInstanceId, InstanceMetaDataResponse::getInstanceStatus));
    }

    public List<InstanceMetaDataResponse> getInstanceMetaDatas() {
        return instanceGroups
                .stream()
                .flatMap(instanceGroup -> instanceGroup.getMetaData().stream())
                .filter(instanceMetadata -> instanceIds.contains(instanceMetadata.getInstanceId()))
                .collect(Collectors.toList());
    }
}