package com.sequenceiq.it.cloudbreak.util.wait.service.redbeams;

import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;
import com.sequenceiq.redbeams.api.model.common.Status;

@Component
public class RedbeamsAwait implements Await<RedbeamsDatabaseServerTestDto, Status> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsAwait.class);

    @Override
    public RedbeamsDatabaseServerTestDto await(RedbeamsDatabaseServerTestDto entity, Status desiredStatus, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Redbeams key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getResponse().getName(), desiredStatus));
            RedbeamsClient client = testContext.getMicroserviceClient(RedbeamsClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String crn = entity.getResponse().getResourceCrn();
            if (desiredStatus.equals(DELETE_COMPLETED)) {
                waitForRedbeamsStatus(new RedbeamsTerminationChecker<>(), client, crn, testContext, desiredStatus,
                        pollingInterval, maxRetry);
            } else {
                waitForRedbeamsStatus(new RedbeamsOperationChecker<>(), client, crn, testContext, desiredStatus,
                        pollingInterval, maxRetry);
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

    private Result<WaitResult, Exception> waitForRedbeamsStatus(ExceptionChecker<RedbeamsWaitObject> statusChecker, RedbeamsClient client,
            String crn, TestContext testContext, Status desiredStatus, Duration pollingInterval, int maxRetry) {
        return testContext.getRedbeamsWaitService().waitObject(
                statusChecker,
                new RedbeamsWaitObject(client, crn, desiredStatus), testContext, pollingInterval, maxRetry, 1);
    }
}
