package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaUserSyncSuccess implements Assertion<FreeIpaUserSyncTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserSyncSuccess.class);

    @Override
    public FreeIpaUserSyncTestDto doAssertion(TestContext testContext, FreeIpaUserSyncTestDto freeIpaUserSyncTestDto, FreeIpaClient freeIpaClient)
            throws Exception {
        SyncOperationStatus syncOperationStatus = freeIpaClient.getDefaultClient()
                .getUserV1Endpoint()
                .getLastSyncOperationStatus(testContext.given(EnvironmentTestDto.class).getCrn());
        if (syncOperationStatus.getFailure() != null && !syncOperationStatus.getFailure().isEmpty()) {
            String failures = StringUtils.join(syncOperationStatus.getFailure(), ", ");
            LOGGER.error(failures);
            throw new TestFailException(failures);
        }

        return freeIpaUserSyncTestDto;
    }
}
