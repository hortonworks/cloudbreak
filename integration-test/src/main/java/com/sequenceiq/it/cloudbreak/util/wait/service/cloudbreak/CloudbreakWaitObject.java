package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.LOAD_BALANCER_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.RESTORE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UNREACHABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class CloudbreakWaitObject implements WaitObject {

    private static final Map<String, Status> STACK_DELETED = Map.of(STATUS, DELETE_COMPLETED);

    private static final Map<String, Status> STACK_FAILED = Map.of(STATUS, AVAILABLE, CLUSTER_STATUS, CREATE_FAILED);

    private final CloudbreakClient client;

    private final String name;

    private final Map<String, Status> desiredStatuses;

    private final Set<Status> ignoredFailedStatuses;

    private final String accountId;

    private StackStatusV4Response stackStatus;

    private final TestContext testContext;

    public CloudbreakWaitObject(CloudbreakClient client, String name, Map<String, Status> desiredStatuses, String accountId, Set<Status> ignoredFailedStatuses,
            TestContext testContext) {
        this.client = client;
        this.name = name;
        this.desiredStatuses = desiredStatuses;
        this.accountId = accountId;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.testContext = testContext;
    }

    public DistroXV1Endpoint getDistroxEndpoint() {
        return client.getDefaultClient(testContext).distroXV1Endpoint();
    }

    public Long getWorkspaceId() {
        return client.getWorkspaceId();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isDeleted() {
        Map<String, Status> deletedStatuses = Map.of(STATUS, DELETE_COMPLETED, CLUSTER_STATUS, DELETE_COMPLETED);
        return deletedStatuses.equals(actualStatusesEnum());
    }

    @Override
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(stackStatus.getStatus()) || ignoredFailedStatuses.contains(stackStatus.getClusterStatus());
    }

    @Override
    public boolean isDeletionInProgress() {
        List<Status> deleteInProgressStatuses = List.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return !ListUtils.retainAll(deleteInProgressStatuses, actualStatusesEnumValues()).isEmpty();
    }

    @Override
    public boolean isCreateFailed() {
        return actualStatusesEnumValues().contains(CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatuses.equals(STACK_DELETED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatuses.equals(STACK_FAILED) || desiredStatuses.get(STATUS).equals(Status.CREATE_FAILED);
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public boolean isFailed() {
        List<Status> failedStatuses = List.of(UPDATE_FAILED, BACKUP_FAILED, RESTORE_FAILED, RECOVERY_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED,
                DELETE_FAILED, START_FAILED, STOP_FAILED, UNREACHABLE, NODE_FAILURE, EXTERNAL_DATABASE_CREATION_FAILED, EXTERNAL_DATABASE_DELETION_FAILED,
                EXTERNAL_DATABASE_START_FAILED, EXTERNAL_DATABASE_STOP_FAILED, LOAD_BALANCER_UPDATE_FAILED);
        return !ListUtils.retainAll(failedStatuses, actualStatusesEnumValues()).isEmpty();
    }

    @Override
    public void fetchData() {
        stackStatus = getDistroxEndpoint().getStatusByName(name);
    }

    @Override
    public boolean isDeleteFailed() {
        return actualStatusesEnumValues().contains(DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (stackStatus == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, stackStatus.getStatus().name(), CLUSTER_STATUS, stackStatus.getClusterStatus().name());
    }

    private Map<String, Status> actualStatusesEnum() {
        if (stackStatus == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, stackStatus.getStatus(), CLUSTER_STATUS, stackStatus.getClusterStatus());
    }

    private List<Status> actualStatusesEnumValues() {
        return new ArrayList<>(actualStatusesEnum().values());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        Map<String, String> ret = new HashMap<>();
        String statusReason = stackStatus.getStatusReason();
        if (statusReason != null) {
            ret.put(STATUS_REASON, statusReason);
        }
        String clusterStatusReason = stackStatus.getClusterStatusReason();
        if (clusterStatusReason != null) {
            ret.put(CLUSTER_STATUS_REASON, clusterStatusReason);
        }
        return ret;
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        Map<String, String> ret = new HashMap<>();
        desiredStatuses.forEach((key, value) -> ret.put(key, value.name()));
        return ret;
    }
}
