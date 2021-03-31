package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIpaSynchronizeAllUsersInternalAction extends AbstractFreeIpaAction<FreeIpaUserSyncTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSynchronizeAllUsersInternalAction.class);

    @Override
    protected FreeIpaUserSyncTestDto freeIpaAction(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" Environment Crn: [%s], freeIpa Crn: %s", testDto.getEnvironmentCrn(), testDto.getRequest().getEnvironments()));
        Log.whenJson(LOGGER, format(" FreeIPA sync internal request: %n"), testDto.getRequest());
        SyncOperationStatus syncOperationStatus = client.getInternalClient(testContext)
                .getUserV1Endpoint()
                .synchronizeAllUsers(testDto.getRequest());
        testDto.setOperationId(syncOperationStatus.getOperationId());
        LOGGER.info("Sync is in state: [{}], sync internal operation: [{}] with type: [{}]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType());
        Log.when(LOGGER, format(" Sync is in state: [%s], sync internal operation: [%s] with type: [%s]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType()));
        return testDto;
    }
}
