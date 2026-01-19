package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaSetPasswordAction implements Action<FreeIpaUserSyncTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSetPasswordAction.class);

    private final String newPassword;

    private final Set<String> environmentCrns;

    public FreeIpaSetPasswordAction(Set<String> environmentCrns, String newPassword) {
        this.environmentCrns = environmentCrns;
        this.newPassword = newPassword;
    }

    public FreeIpaUserSyncTestDto action(TestContext testContext, FreeIpaUserSyncTestDto testDto, FreeIpaClient client) throws Exception {
        SetPasswordRequest setPasswordRequest = testDto.setPassword(environmentCrns, newPassword);
        Log.when(LOGGER, format(" List of environment Crns: [%s], freeIpa Crn: %s", environmentCrns, testDto.getRequest().getEnvironments()));
        Log.whenJson(LOGGER, format(" FreeIPA set password request: %n"), setPasswordRequest);
        SyncOperationStatus syncOperationStatus = client.getDefaultClient(testContext)
                .getUserV1Endpoint()
                .setPassword(setPasswordRequest);
        testDto.setOperationId(syncOperationStatus.getOperationId());
        LOGGER.info("Sync is in state: [{}], sync operation: [{}] with type: [{}]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType());
        Log.when(LOGGER, format(" Sync is in state: [%s], sync operation: [%s] with type: [%s]", syncOperationStatus.getStatus(),
                syncOperationStatus.getOperationId(), syncOperationStatus.getSyncOperationType()));
        return testDto;
    }
}
