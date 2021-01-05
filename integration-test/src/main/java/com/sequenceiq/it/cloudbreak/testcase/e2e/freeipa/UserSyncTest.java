package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncStatusDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class UserSyncTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncTest.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running environment",
            when = "an user sync status request sent",
            then = "there should be a finished user sync with duration lower than the given value")
    public void testUserSyncDuration(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        testContext.given(freeIpa, FreeIpaTestDto.class)
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(Status.AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .await(UserSyncState.UP_TO_DATE)
                .validate();
        testContext.given(FreeIpaUserSyncStatusDto.class)
                .when(freeIpaTestClient.getLastUserSyncStatus())
                .withEnvironmentCrn()
                .then((tc, testDto, client) -> {
                    LOGGER.info(String.format("Freeipa last user sync status is %s", testDto.getResponse().getStatus()));
                    Assertions.assertThat(SynchronizationStatus.COMPLETED.equals(testDto.getResponse().getStatus()));
                    long duration = testDto.getResponse().getEndTime() - testDto.getResponse().getStartTime();
                    LOGGER.info(String.format("Freeipa last user sync duration took %d sec", TimeUnit.MILLISECONDS.toSeconds(duration)));
                    Assertions.assertThat(duration < TimeUnit.MINUTES.toMillis(5));
                    List<FailureDetails> failures = testDto.getResponse().getFailure();
                    if (failures != null && !failures.isEmpty()) {
                        LOGGER.info(String.format("Freeipa last user sync had the following warnings: %s", Joiner.on(",").join(failures)));
                    }
                    Assertions.assertThat(failures.isEmpty());
                    return testDto;
                })
                .validate();
    }
}
