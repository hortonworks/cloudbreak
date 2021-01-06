package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncStatusDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;

public class UserSyncTest extends AbstractMockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        //hacky way to let access to image catalog
        initializeDefaultBlueprints(testContext);
        createDefaultImageCatalog(testContext);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running datalake in an environment",
            when = "a user sync status request sent",
            then = "there should be a finished user sync with duration lower than the given value")
    public void testUserSyncDuration(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIpa(testContext);
        testContext.given(FreeIpaTestDto.class)
                .withCatalog(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .validate();
        createDatalake(testContext);
        testContext.given(FreeIpaUserSyncStatusDto.class)
                .when(freeIpaTestClient.getLastUserSyncStatus())
                .withEnvironmentCrn()
                .then((tc, testDto, client) -> {
                    LOGGER.info(String.format("Freeipa last user sync status is %s", testDto.getResponse().getStatus()));
                    Assertions.assertThat(UserSyncState.UP_TO_DATE.equals(testDto.getResponse().getStatus()));
                    long duration = testDto.getResponse().getEndTime() - testDto.getResponse().getStartTime();
                    LOGGER.info(String.format("Freeipa last user sync duration took %d sec", TimeUnit.SECONDS.toMinutes(duration)));
                    Assertions.assertThat(duration < TimeUnit.MINUTES.toMillis(10));
                    return testDto;
                })
                .validate();
    }
}
