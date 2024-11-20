package com.sequenceiq.freeipa.service.polling.usersync;

import java.util.List;
import java.util.Objects;

public class CloudIdSyncPollerObject {

    private final String environmentCrn;

    private final List<Long> commandIds;

    public CloudIdSyncPollerObject(String environmentCrn, List<Long> commandIds) {
        this.environmentCrn = environmentCrn;
        this.commandIds = commandIds;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public List<Long> getCommandIds() {
        return commandIds;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            CloudIdSyncPollerObject that = (CloudIdSyncPollerObject) o;
            return Objects.equals(environmentCrn, that.environmentCrn) && Objects.equals(commandIds, that.commandIds);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentCrn, commandIds);
    }
}
