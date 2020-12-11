package com.sequenceiq.it.cloudbreak.util.wait.service.redbeams;

import static com.sequenceiq.redbeams.api.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_REQUESTED;
import static com.sequenceiq.redbeams.api.model.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.PRE_DELETE_IN_PROGRESS;
import static com.sequenceiq.redbeams.api.model.common.Status.START_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.STOP_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_FAILED;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsWaitObject implements WaitObject {

    private final RedbeamsClient client;

    private final String crn;

    private final Status desiredStatus;

    private DatabaseServerV4Response redbeams;

    public RedbeamsWaitObject(RedbeamsClient client, String crn, Status desiredStatus) {
        this.client = client;
        this.crn = crn;
        this.desiredStatus = desiredStatus;
    }

    public DatabaseServerV4Endpoint getEndpoint() {
        return client.getEndpoints().databaseServerV4Endpoint();
    }

    public String getCrn() {
        return crn;
    }

    public Status getDesiredStatus() {
        return desiredStatus;
    }

    @Override
    public void fetchData() {
        redbeams = getEndpoint().getByCrn(crn);
    }

    @Override
    public boolean isDeleteFailed() {
        return redbeams.getStatus().equals(DELETE_FAILED);
    }

    @Override
    public Map<String, String> actualStatuses() {
        return Map.of("status", redbeams.getStatus().name());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return Map.of("statusReason", redbeams.getStatusReason());
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return Map.of("status", desiredStatus.name());
    }

    @Override
    public String getName() {
        return redbeams.getName();
    }

    @Override
    public boolean isDeleted() {
        return redbeams.getStatus().equals(DELETE_COMPLETED);
    }

    @Override
    public boolean isFailed() {
        Set<Status> failedStatuses = Set.of(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED);
        return failedStatuses.contains(redbeams.getStatus());
    }

    @Override
    public boolean isDeletionInProgress() {
        Set<Status> deleteInProgressStatuses = Set.of(DELETE_REQUESTED, PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS);
        return deleteInProgressStatuses.contains(redbeams.getStatus());
    }

    @Override
    public boolean isCreateFailed() {
        return redbeams.getStatus().equals(CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatus.equals(DELETE_COMPLETED);
    }

    @Override
    public boolean isFailedCheck() {
        return false;
    }
}
