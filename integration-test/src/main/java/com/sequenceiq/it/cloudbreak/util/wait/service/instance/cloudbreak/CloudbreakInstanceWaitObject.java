package com.sequenceiq.it.cloudbreak.util.wait.service.instance.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ZOMBIE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;

public class CloudbreakInstanceWaitObject implements InstanceWaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakInstanceWaitObject.class);

    private final String name;

    private final List<String> instanceIds;

    private final InstanceStatus desiredStatus;

    private final TestContext testContext;

    private List<InstanceGroupV4Response> instanceGroups;

    private String instanceResourceType = "Data Lake";

    public CloudbreakInstanceWaitObject(TestContext testContext, String name, List<String> instanceIds, InstanceStatus desiredStatus) {
        this.testContext = testContext;
        this.name = name;
        this.instanceIds = instanceIds;
        this.desiredStatus = desiredStatus;
    }

    @Override
    public void fetchData() {
        StackV4Response stackResponse = null;
        try {
            stackResponse = testContext.getMicroserviceClient(CloudbreakClient.class)
                    .getDefaultClient(testContext).distroXV1Endpoint().getByName(name, Set.of());
            instanceGroups = stackResponse.getInstanceGroups();
            instanceResourceType = "Data Hub";
        } catch (NotFoundException e) {
            LOGGER.info("SDX '{}' instance groups are present for validation.", name);
            stackResponse = testContext.getSdxClient().getDefaultClient(testContext).sdxEndpoint().getDetail(name, Set.of()).getStackV4Response();
            instanceGroups = stackResponse.getInstanceGroups();
        } catch (Exception e) {
            String errorMsg = "Instance groups cannot be determined.";
            if (stackResponse != null) {
                errorMsg += " Stack status: " + stackResponse.getStatus().name() +
                        " Stack status reason: " + stackResponse.getStatusReason();
            }
            LOGGER.error(errorMsg, e);
            throw new TestFailException(errorMsg, e);
        }
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (instanceGroups == null) {
            return Collections.emptyMap();
        }
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId,
                        instanceMetaData -> instanceMetaData.getInstanceStatus().name()));
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId,
                        instanceMetaData -> {
                            String statusReason = instanceMetaData.getStatusReason();
                            if (StringUtils.isBlank(statusReason)) {
                                return String.format("Status reason is NOT available for '%s' instance!", instanceResourceType);
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

    public String getName() {
        return name;
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
        return getInstanceStatuses().values().stream().anyMatch(DECOMMISSION_FAILED::equals);
    }

    @Override
    public boolean isFailed() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED, ZOMBIE);
        return getInstanceStatuses().values().stream().anyMatch(failedStatuses::contains);
    }

    @Override
    public boolean isDeletionInProgress() {
        return getInstanceStatuses().values().stream().allMatch(DELETE_REQUESTED::equals);
    }

    @Override
    public boolean isCreateFailed() {
        Set<InstanceStatus> failedStatuses = Set.of(ORCHESTRATION_FAILED, ZOMBIE);
        return getInstanceStatuses().values().stream().anyMatch(failedStatuses::contains);
    }

    @Override
    public boolean isDeletionCheck() {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return deletedStatuses.contains(desiredStatus);
    }

    @Override
    public boolean isFailedCheck() {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED, ZOMBIE);
        return failedStatuses.contains(desiredStatus);
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    @Override
    public Map<String, String> getFetchedInstanceStatuses() {
        return getInstanceMetaDatas().stream().collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId,
                instanceMetaData -> instanceMetaData.getInstanceStatus().name()));
    }

    @Override
    public List<String> getFetchedInstanceIds() {
        return instanceGroups
                .stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getInstanceId)
                .collect(Collectors.toList());
    }

    public Map<String, InstanceStatus> getInstanceStatuses() {
        return getInstanceMetaDatas().stream()
                .collect(Collectors.toMap(InstanceMetaDataV4Response::getInstanceId, InstanceMetaDataV4Response::getInstanceStatus));
    }

    public List<InstanceMetaDataV4Response> getInstanceMetaDatas() {
        return instanceGroups
                .stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(instanceMetadata -> instanceIds.contains(instanceMetadata.getInstanceId()))
                .collect(Collectors.toList());
    }
}
