package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaSynchronizeAllUsersInternalAction extends AbstractFreeIpaAction<FreeIpaUserSyncTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSynchronizeAllUsersInternalAction.class);

    private static final String PARTIAL_SYNC_NOT_ENABLED_ERROR = "Partial sync is not available for CDP SAAS.";

    @Override
    protected FreeIpaUserSyncTestDto freeIpaAction(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" Environment Crn: [%s], freeIpa Crn: %s", testDto.getEnvironmentCrn(), testDto.getRequest().getEnvironments()));
        Log.whenJson(LOGGER, format(" FreeIPA sync internal request: %n"), testDto.getRequest());
        SyncOperationStatus syncOperationStatus;
        try {
            syncOperationStatus = client.getInternalClient(testContext)
                    .getUserV1Endpoint()
                    .synchronizeAllUsers(testDto.getRequest());
        } catch (BadRequestException e) {
            String message = e.getResponse().readEntity(String.class);
            if (message != null && message.contains(PARTIAL_SYNC_NOT_ENABLED_ERROR)) {
                LOGGER.info(PARTIAL_SYNC_NOT_ENABLED_ERROR);
                testDto.setErrorMessage(PARTIAL_SYNC_NOT_ENABLED_ERROR);
                return testDto;
            } else {
                throw e;
            }
        }
        testDto.setOperationId(syncOperationStatus.getOperationId());
        LOGGER.info("Sync is in state: [{}], sync internal operation: [{}] with type: [{}]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType());
        Log.when(LOGGER, format(" Sync is in state: [%s], sync internal operation: [%s] with type: [%s]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType()));
        return testDto;
    }
}
