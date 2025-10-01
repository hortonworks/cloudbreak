package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaUserSyncDoneWithNoFailures;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaUserSyncDurationLessThan;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class UserSyncTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default environment with no freeIpa",
            when = "freeIpa should be successfully created",
            then = "all users should be synced successfully with no failure " +
                    "and finished in given (5) minutes")
    public void testUserSyncDuration(TestContext testContext) {
        testContext.given(FreeIpaTestDto.class)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .then(new FreeIpaUserSyncDurationLessThan(5))
                .then(new FreeIpaUserSyncDoneWithNoFailures())
                .validate();
    }
}
