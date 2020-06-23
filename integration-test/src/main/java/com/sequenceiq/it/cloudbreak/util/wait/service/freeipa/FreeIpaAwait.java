package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class FreeIpaAwait implements Await<FreeIpaTestDto, Status> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAwait.class);

    @Override
    public FreeIpaTestDto await(FreeIpaTestDto entity, Status desiredStatus, TestContext testContext, RunningParameter runningParameter,
            Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("FreeIpa key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getResponse().getName(), desiredStatus));
            FreeIpaClient client = testContext.getMicroserviceClient(FreeIpaClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String environmentCrn = entity.getResponse().getEnvironmentCrn();
            if (desiredStatus.equals(DELETE_COMPLETED)) {
                waitForFreeIpaStatus(new FreeIpaTerminationChecker<>(), client, environmentCrn, testContext, desiredStatus,
                        pollingInterval, maxRetry);
            } else {
                waitForFreeIpaStatus(new FreeIpaOperationChecker<>(), client, environmentCrn, testContext, desiredStatus,
                        pollingInterval, maxRetry);
                entity.refresh(testContext, null);
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

    private Result<WaitResult, Exception> waitForFreeIpaStatus(ExceptionChecker<FreeIpaWaitObject> statusChecker, FreeIpaClient client,
            String environmentCrn, TestContext testContext, Status desiredStatus, Duration pollingInterval, int maxRetry) {
        return testContext.getFreeIpaWaitService().waitObject(
                statusChecker,
                new FreeIpaWaitObject(client, environmentCrn, desiredStatus), testContext, pollingInterval, maxRetry, 1);
    }
}
