package com.sequenceiq.it.cloudbreak.assertion.freeipa;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class FreeIpaUserSyncDoneWithNoFailures implements Assertion<FreeIpaUserSyncTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUserSyncDoneWithNoFailures.class);

    @Override
    public FreeIpaUserSyncTestDto doAssertion(TestContext testContext, FreeIpaUserSyncTestDto freeIpaUserSyncTestDto, FreeIpaClient freeIpaClient)
            throws Exception {
        List<FailureDetails> failures = freeIpaUserSyncTestDto.getResponse().getFailure();
        String message = String.format("FreeIpa last user sync have been failed with following issues: %s", Joiner.on(",").join(failures));

        if (CollectionUtils.isNotEmpty(failures)) {
            LOGGER.error(message);
            throw new TestFailException(message);
        }

        return freeIpaUserSyncTestDto;
    }
}
