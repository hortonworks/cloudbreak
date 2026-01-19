package com.sequenceiq.it.cloudbreak.await.sdx;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.it.cloudbreak.await.Await;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.endpoint.SdxRecoveryEndpoint;
import com.sequenceiq.sdx.api.model.SdxResizeOperationResponse;

public class SdxResizeAwait implements Await<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeAwait.class);

    @Override
    public SdxInternalTestDto await(TestContext testContext, SdxInternalTestDto testDto, SdxClient client, RunningParameter runningParameter) {
        SdxRecoveryEndpoint sdxRecoveryEndpoint = client.getDefaultClient(testContext).sdxRecoveryEndpoint();
        try {
            Polling.stopAfterAttempt(testContext.getMaxRetry())
                    .waitPeriodly(testContext.getPollingInterval(), TimeUnit.MILLISECONDS)
                    .run(() -> checkResizeOperation(testContext, testDto, sdxRecoveryEndpoint));
        } catch (Exception e) {
            testContext.getExceptionMap().put(String.format("Cloudbreak await for Data Lake resize %s", testDto), e);
            LOGGER.error("Failure while waited for resize operation. Message: {}", e.getMessage(), e);
        }
        return testDto;
    }

    private static AttemptResult<Void> checkResizeOperation(TestContext testContext, SdxInternalTestDto testDto, SdxRecoveryEndpoint sdxRecoveryEndpoint) {
        SdxResizeOperationResponse resize = sdxRecoveryEndpoint.getResizeStatus(testDto.getResponse().getEnvironmentCrn());
        if (resize == null || resize.getOperationId() == null) {
            LOGGER.info("Resize operation is not found.");
            return AttemptResults.justFinish();
        } else if (Boolean.TRUE.equals(resize.getActive())) {
            LOGGER.info("Resize operation {} is active.", resize.getOperationId());
            return AttemptResults.justContinue();
        } else if (Boolean.TRUE.equals(resize.getFailed())) {
            LOGGER.info("Resize operation {} failed. Reason: {}", resize.getOperationId(), resize.getStatusReason());
            return AttemptResults.breakFor(String.format("Resize operation %s failed. Reason: %s", resize.getOperationId(), resize.getStatusReason()));
        } else {
            LOGGER.info("Resize operation {} has completed.", resize.getOperationId());
            return AttemptResults.justFinish();
        }
    }
}
