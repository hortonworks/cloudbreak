package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import java.util.Map;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;

public class FreeIpaUserSyncWaitObject extends FreeIpaWaitObject {

    private final UserSyncState desiredUserSyncState;

    private EnvironmentUserSyncState environmentUserSyncState;

    public FreeIpaUserSyncWaitObject(FreeIpaClient freeIpaClient, String freeipaName, String environmentCrn,
            UserSyncState desiredUserSyncState) {
        super(freeIpaClient, freeipaName, environmentCrn, Status.AVAILABLE);
        this.desiredUserSyncState = desiredUserSyncState;
    }

    @Override
    public void fetchData() {
        super.fetchData();
        environmentUserSyncState = getClient().getDefaultClient().getUserV1Endpoint().getUserSyncState(getEnvironmentCrn());
    }

    @Override
    public Map<String, String> actualStatuses() {
        return Map.of(STATUS, environmentUserSyncState.getState().name());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return Map.of();
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        return Map.of(STATUS, desiredUserSyncState.name());
    }

    @Override
    public String getName() {
        return super.getEnvironmentCrn() + " - userSync";
    }

}