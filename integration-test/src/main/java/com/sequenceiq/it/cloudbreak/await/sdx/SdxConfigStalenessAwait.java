package com.sequenceiq.it.cloudbreak.await.sdx;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.common.api.type.ConfigStalenessState;
import com.sequenceiq.it.cloudbreak.await.Await;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxConfigStalenessAwait implements Await<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxConfigStalenessAwait.class);

    private final ConfigStalenessState configStalenessState;

    public SdxConfigStalenessAwait(ConfigStalenessState configStalenessState) {
        this.configStalenessState = configStalenessState;
    }

    @Override
    public SdxInternalTestDto await(TestContext testContext, SdxInternalTestDto testDto, SdxClient client, RunningParameter runningParameter) {
        try {
            Polling.stopAfterAttempt(testContext.getMaxRetry())
                    .waitPeriodly(testContext.getPollingInterval(), TimeUnit.MILLISECONDS)
                    .run(() -> checkConfigStaleness(testContext, testDto));
        } catch (Exception e) {
            testContext.getExceptionMap().put(String.format("Cloudbreak await for Data Lake config staleness %s", testDto), e);
            LOGGER.error("Failure while waited for config staleness. Message: {}", e.getMessage(), e);
        }
        return testDto;
    }

    private AttemptResult<Void> checkConfigStaleness(TestContext testContext, SdxInternalTestDto testDto) {
        testDto.refresh();
        String configStalenessState = testDto.getResponse().getConfigStaleness().getState();
        LOGGER.debug("Current Data Lake config staleness state: {}", configStalenessState);
        return Objects.equals(configStalenessState, this.configStalenessState.name())
                ? AttemptResults.justFinish()
                : AttemptResults.justContinue();
    }
}
