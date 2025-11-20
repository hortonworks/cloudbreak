package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState.STALE;
import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState.SYNC_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState.SYNC_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState.UP_TO_DATE;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusRequest;
import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaUserSyncWaitObject extends FreeIpaWaitObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserSyncWaitObject.class);

    private final UserSyncState desiredUserSyncState;

    private final Set<UserSyncState> ignoredFailedStatuses;

    private final TestContext testContext;

    private EnvironmentUserSyncState environmentUserSyncState;

    public FreeIpaUserSyncWaitObject(FreeIpaClient freeIpaClient, String freeipaName, String environmentCrn,
            UserSyncState desiredUserSyncState, Set<UserSyncState> ignoredFailedStatuses, TestContext testContext) {
        super(freeIpaClient, freeipaName, environmentCrn, Status.AVAILABLE);
        this.desiredUserSyncState = desiredUserSyncState;
        this.ignoredFailedStatuses = ignoredFailedStatuses;
        this.testContext = testContext;
    }

    @Override
    public void fetchData() {
        super.fetchData();
        String environmentCrn = getEnvironmentCrn();
        environmentUserSyncState = getClient().getDefaultClient().getUserV1Endpoint().getUserSyncState(environmentCrn);
        if (environmentUserSyncState.getState() == UserSyncState.STALE) {
            try {
                LOGGER.info("FreeIPA user sync state is STALE, try to fetch from public env api");
                EnvironmentPublicApiClient environmentPublicApiClient = testContext.getMicroserviceClient(EnvironmentPublicApiClient.class);
                LastSyncStatusResponse lastSyncStatusResponse = environmentPublicApiClient.getDefaultClient()
                        .lastSyncStatus(new LastSyncStatusRequest().environment(environmentCrn));
                if (Optional.ofNullable(lastSyncStatusResponse).map(LastSyncStatusResponse::getStatus).orElse(SyncStatus.NEVER_RUN) != SyncStatus.NEVER_RUN) {
                    environmentUserSyncState = mapEnvironmentUserSyncState(lastSyncStatusResponse);
                }
            } catch (Exception e) {
                Log.warn(LOGGER, " FreeIPA usersync fetching from public environment API failed", e);
            }
        }
    }

    private EnvironmentUserSyncState mapEnvironmentUserSyncState(LastSyncStatusResponse lastSyncStatusResponse) {
        UserSyncState userSyncState = switch (lastSyncStatusResponse.getStatus()) {
            case REJECTED, FAILED, TIMEDOUT -> SYNC_FAILED;
            case RUNNING, REQUESTED -> SYNC_IN_PROGRESS;
            case COMPLETED -> UP_TO_DATE;
            default -> STALE;
        };
        EnvironmentUserSyncState state = new EnvironmentUserSyncState();
        state.setState(userSyncState);
        state.setLastUserSyncOperationId(lastSyncStatusResponse.getOperationId());
        return state;
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (environmentUserSyncState == null || environmentUserSyncState.getState() == null) {
            return Collections.emptyMap();
        }
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
    public boolean isFailedButIgnored() {
        return ignoredFailedStatuses.contains(environmentUserSyncState.getState());
    }

    @Override
    public String getName() {
        return super.getEnvironmentCrn() + " - userSync";
    }

}