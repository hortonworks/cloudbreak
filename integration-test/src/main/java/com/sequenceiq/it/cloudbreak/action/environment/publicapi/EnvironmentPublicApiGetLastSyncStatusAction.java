package com.sequenceiq.it.cloudbreak.action.environment.publicapi;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import com.cloudera.thunderhead.service.environments2api.ApiException;
import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatus;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiLastSyncStatusDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

public class EnvironmentPublicApiGetLastSyncStatusAction implements Action<EnvironmentPublicApiLastSyncStatusDto, EnvironmentPublicApiClient> {
    @Override
    public EnvironmentPublicApiLastSyncStatusDto action(TestContext testContext, EnvironmentPublicApiLastSyncStatusDto testDto,
            EnvironmentPublicApiClient client) throws Exception {
        testDto.setResponse(fetchLastSyncStatusResponse(testDto, client));
        Log.whenJson("Environment last sync status response: ", testDto.getResponse());
        return testDto;
    }

    private LastSyncStatusResponse fetchLastSyncStatusResponse(EnvironmentPublicApiLastSyncStatusDto testDto, EnvironmentPublicApiClient client)
            throws ApiException {
        try {
            return client.getDefaultClient().lastSyncStatus(testDto.getRequest());
        } catch (NotFoundException e) {
            return new LastSyncStatusResponse().status(SyncStatus.NEVER_RUN);
        } catch (ApiException e) {
            if (e.getCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                return new LastSyncStatusResponse().status(SyncStatus.NEVER_RUN);
            } else {
                throw e;
            }
        }
    }
}
