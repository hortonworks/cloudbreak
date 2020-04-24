package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class EnvironmentAwait implements Await<EnvironmentTestDto, EnvironmentStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentAwait.class);

    @Override
    public EnvironmentTestDto await(EnvironmentTestDto entity, EnvironmentStatus desiredStatus, TestContext testContext, RunningParameter runningParameter,
            long pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Environment key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getResponse().getName(), desiredStatus));
            EnvironmentClient client = testContext.getMicroserviceClient(EnvironmentClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String crn = entity.getResponse().getCrn();
            switch (desiredStatus) {
                case AVAILABLE:
                case ENV_STOPPED:
                    waitForEnvironmentStatus(new EnvironmentOperationChecker<>(), client, crn, testContext, desiredStatus,
                            pollingInterval, maxRetry);
                    break;
                case ARCHIVED:
                    waitForEnvironmentStatus(new EnvironmentTerminationChecker<>(), client, crn, testContext, desiredStatus,
                            pollingInterval, maxRetry);
                    break;
                default:
                    LOGGER.warn("Wait checker is not implemented yet for the desired environment state '{}' ", desiredStatus);
                    break;
            }
            testContext.setStatuses(Map.of("status", entity.getResponse().getEnvironmentStatus().name()));
            if (!desiredStatus.equals(ARCHIVED)) {
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

    private Result<WaitResult, Exception> waitForEnvironmentStatus(ExceptionChecker<EnvironmentWaitObject> statusChecker, EnvironmentClient client,
            String crn, TestContext testContext, EnvironmentStatus desiredStatus, long pollingInterval, int maxRetry) {
        return testContext.getEnvironmentWaitService().waitWithTimeout(
                statusChecker,
                new EnvironmentWaitObject(client, crn, desiredStatus),
                pollingInterval, maxRetry, 1);
    }
}
