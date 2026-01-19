package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.environments2api.model.LastSyncStatusResponse;
import com.cloudera.thunderhead.service.environments2api.model.SyncStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.it.cloudbreak.client.EnvironmentPublicApiTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.envpublicapi.EnvironmentPublicApiLastSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaGetLastSyncOperationStatus extends AbstractFreeIpaAction<FreeIpaUserSyncTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaGetLastSyncOperationStatus.class);

    private final EnvironmentPublicApiTestClient environmentPublicApiTestClient;

    public FreeIpaGetLastSyncOperationStatus(EnvironmentPublicApiTestClient environmentPublicApiTestClient) {
        this.environmentPublicApiTestClient = environmentPublicApiTestClient;
    }

    @Override
    protected FreeIpaUserSyncTestDto freeIpaAction(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" Environment Crn: [%s], freeIpa Crn: %s", testDto.getEnvironmentCrn(), testDto.getRequest().getEnvironments()));
        Log.whenJson(LOGGER, format(" FreeIPA get last sync status request: %n"), testDto.getRequest());
        SyncOperationStatus syncOperationStatus = getSyncOperationStatus(testContext, testDto, client);
        testDto.setOperationId(syncOperationStatus.getOperationId());
        testDto.setResponse(syncOperationStatus);
        LOGGER.info("Last sync is in state: [{}], last sync operation: [{}] with type: [{}]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType());
        Log.when(LOGGER, format(" Last sync is in state: [%s], last sync operation: [%s] with type: [%s]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType()));
        return testDto;
    }

    private SyncOperationStatus getSyncOperationStatus(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) {
        try {
            return client.getDefaultClient(testContext)
                    .getUserV1Endpoint()
                    .getLastSyncOperationStatus(testDto.getEnvironmentCrn());
        } catch (jakarta.ws.rs.NotFoundException e) {
            LOGGER.warn("User sync status not found in FMS, running query against public environment API");
            LastSyncStatusResponse statusResponse = testContext.given(EnvironmentPublicApiLastSyncStatusDto.class)
                    .when(environmentPublicApiTestClient.getLastSyncStatus())
                    .getResponse();
            SynchronizationStatus synchronizationStatus = getSynchronizationStatus(statusResponse);
            String operationId = statusResponse.getOperationId();
            return new SyncOperationStatus(operationId, SyncOperationType.USER_SYNC, synchronizationStatus, List.of(), List.of(), null, 0L, 0L);
        }

    }

    private SynchronizationStatus getSynchronizationStatus(LastSyncStatusResponse statusResponse) {
        try {
            return SynchronizationStatus.valueOf(statusResponse.getStatus().name());
        } catch (IllegalArgumentException ex) {
            return statusResponse.getStatus() == SyncStatus.NEVER_RUN ? SynchronizationStatus.REQUESTED : SynchronizationStatus.FAILED;
        }
    }
}
