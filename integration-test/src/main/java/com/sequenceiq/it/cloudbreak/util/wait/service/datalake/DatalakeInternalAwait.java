package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class DatalakeInternalAwait implements Await<SdxInternalTestDto, SdxClusterStatusResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeInternalAwait.class);

    @Override
    public SdxInternalTestDto await(SdxInternalTestDto entity, SdxClusterStatusResponse desiredStatus, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Datalake Internal key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatus));
            SdxClient client = testContext.getMicroserviceClient(SdxClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String name = entity.getName();
            if (desiredStatus.equals(DELETED)) {
                waitForDatalakeStatus(new DatalakeTerminationChecker<>(), client, name, testContext, desiredStatus,
                        pollingInterval, maxRetry);
            } else {
                waitForDatalakeStatus(new DatalakeOperationChecker<>(), client, name, testContext, desiredStatus,
                        pollingInterval, maxRetry);
                entity.refresh();
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatus, ResponseUtil.getErrorMessage(e),
                        entity.getName());
                Log.await(null, String.format("[%s] is failed for statuses %s: %s, name: %s",
                        entity, desiredStatus, ResponseUtil.getErrorMessage(e), entity.getName()));
            }
            testContext.getExceptionMap().put("await " + entity + " for desired statuses " + desiredStatus, e);
        }
        return entity;
    }

    private Result<WaitResult, Exception> waitForDatalakeStatus(ExceptionChecker<DatalakeWaitObject> statusChecker, SdxClient client, String name,
            TestContext testContext, SdxClusterStatusResponse desiredStatus, Duration pollingInterval, int maxRetry) {
        return testContext.getDatalakeWaitService().waitObject(
                statusChecker,
                new DatalakeWaitObject(client, name, desiredStatus), testContext, pollingInterval, maxRetry, 1);
    }
}
