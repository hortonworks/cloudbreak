package com.sequenceiq.cloudbreak.datalakedr.model;

import java.util.Optional;

public class DatalakeRestoreStatusResponse extends DatalakeBackupStatusResponse {

    private final String restoreId;

    public DatalakeRestoreStatusResponse(String backupId, String restoreId, State state, Optional<String> failureReason) {
        super(backupId, state, failureReason);
        this.restoreId = restoreId;
    }

    public String getRestoreId() {
        return restoreId;
    }
}
