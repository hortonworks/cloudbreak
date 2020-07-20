package com.sequenceiq.freeipa.service.polling.usersync;

import java.util.Objects;

public class CloudIdSyncPollerObject {

    private final String environmentCrn;

    private final long commandId;

    public CloudIdSyncPollerObject(String environmentCrn, long commandId) {
        this.environmentCrn = environmentCrn;
        this.commandId = commandId;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public long getCommandId() {
        return commandId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CloudIdSyncPollerObject that = (CloudIdSyncPollerObject) o;
        return commandId == that.commandId &&
                Objects.equals(environmentCrn, that.environmentCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentCrn, commandId);
    }
}
