package com.sequenceiq.it.cloudbreak.util.wait.service.redbeams;

import static com.sequenceiq.redbeams.api.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.START_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.STOP_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_FAILED;

import java.util.Set;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsWaitObject {

    private final RedbeamsClient client;

    private final String crn;

    private final Status desiredStatus;

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

    public boolean isFailed(Status redbeamsStatus) {
        Set<Status> failedStatuses = Set.of(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED);
        return failedStatuses.contains(redbeamsStatus);
    }
}
