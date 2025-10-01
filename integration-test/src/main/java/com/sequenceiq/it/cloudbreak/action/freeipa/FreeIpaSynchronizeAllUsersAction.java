package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import jakarta.ws.rs.ClientErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.EnvironmentUserSyncState;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaSynchronizeAllUsersAction implements Action<FreeIpaUserSyncTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSynchronizeAllUsersAction.class);

    public FreeIpaUserSyncTestDto action(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" Environment Crn: [%s], freeIpa Crn: %s", environmentCrn, testDto.getRequest().getEnvironments()));
        Log.whenJson(LOGGER, format(" FreeIPA sync request: %n"), testDto.getRequest());
        // checking if there is ongoing usersync
        EnvironmentUserSyncState environmentUserSyncState =
                client.getDefaultClient().getUserV1Endpoint().getUserSyncState(environmentCrn);
        if (UserSyncState.SYNC_IN_PROGRESS.equals(environmentUserSyncState.getState())) {
            // sync already in progress, no need to execute another one
            testDto.setOperationId(environmentUserSyncState.getLastUserSyncOperationId());
            LOGGER.info("Sync is in state: [{}], sync operation: [{}]", environmentUserSyncState.getState(),
                    environmentUserSyncState.getLastUserSyncOperationId());
            Log.when(LOGGER, format(" Sync is in state: [%s], sync operation: [%s]", environmentUserSyncState.getState(),
                    environmentUserSyncState.getLastUserSyncOperationId()));
        } else {
            try {
                // need to sync
                SyncOperationStatus syncOperationStatus = client.getDefaultClient()
                        .getUserV1Endpoint()
                        .synchronizeAllUsers(testDto.getRequest());
                testDto.setOperationId(syncOperationStatus.getOperationId());
                LOGGER.info("Sync is in state: [{}], sync operation id: [{}] with type: [{}]", syncOperationStatus.getStatus(),
                        syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType());
                Log.when(LOGGER, format(" Sync is in state: [%s], sync operation id: [%s] with type: [%s]", syncOperationStatus.getStatus(),
                        syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType()));
            } catch (ClientErrorException e) {
                // still can happen that a concurrent user sync got initiated
                if (e.getResponse() != null && HttpStatus.CONFLICT.value() == e.getResponse().getStatus()) {
                    LOGGER.info("Sync were already initiated for environment {}", environmentCrn);
                } else {
                    throw e;
                }
            }
        }
        return testDto;
    }
}
