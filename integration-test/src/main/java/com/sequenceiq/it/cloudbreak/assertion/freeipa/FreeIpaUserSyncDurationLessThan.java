package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class FreeIpaUserSyncDurationLessThan implements Assertion<FreeIpaUserSyncTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserSyncDurationLessThan.class);

    private final long expectedDuration;

    public FreeIpaUserSyncDurationLessThan(long expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    @Override
    public FreeIpaUserSyncTestDto doAssertion(TestContext testContext, FreeIpaUserSyncTestDto freeIpaUserSyncTestDto, FreeIpaClient freeIpaClient)
            throws Exception {
        long actualDuration = freeIpaUserSyncTestDto.getResponse().getEndTime() - freeIpaUserSyncTestDto.getResponse().getStartTime();
        String message = String.format("FreeIpa last user sync have been took (%d) more than the expected %d minutes!",
                TimeUnit.MILLISECONDS.toMinutes(actualDuration), expectedDuration);

        if (actualDuration > TimeUnit.MINUTES.toMillis(expectedDuration)) {
            LOGGER.error(message);
            throw new TestFailException(message);
        }

        return freeIpaUserSyncTestDto;
    }
}
