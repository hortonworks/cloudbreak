package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncStatusDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIpaGetLastUserSyncStatusAction implements Action<FreeIpaUserSyncStatusDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaGetLastUserSyncStatusAction.class);

    @Override
    public FreeIpaUserSyncStatusDto action(TestContext testContext, FreeIpaUserSyncStatusDto testDto, FreeIpaClient client) throws Exception {
        testDto.setResponse(
                client.getFreeIpaClient().getUserV1Endpoint().getLastSyncOperationStatus(testDto.getRequest())
        );
        Log.whenJson(LOGGER, " FreeIPA getLastSyncStatus response: ", testDto.getResponse());
        return testDto;
    }
}
