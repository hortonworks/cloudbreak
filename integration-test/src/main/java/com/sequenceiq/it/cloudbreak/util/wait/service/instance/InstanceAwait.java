package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.cloudbreak.CloudbreakInstanceWaitObject;

@Component
public class InstanceAwait {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceAwait.class);

    public <E extends Enum<E>> CloudbreakTestDto await(CloudbreakTestDto entity, Map<List<String>, E> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter) {
        if (entity == null) {
            throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
        }
        try {
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            Duration pollingInterval = testContext.getPollingDurationOrTheDefault(runningParameter);
            int maxRetry = testContext.getMaxRetry();
            MicroserviceClient client = testContext.getMicroserviceClient(entity.getClass(), testContext.setActingUser(runningParameter).getAccessKey());

            desiredStatuses.forEach((instanceIds, instanceStatus) -> {
                InstanceWaitObject instanceWaitObject = client.waitInstancesObject(entity, testContext, instanceIds, instanceStatus);

                if (instanceWaitObject.isDeletionCheck()) {
                    client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceTerminationChecker<>(), instanceWaitObject, testContext,
                            pollingInterval, maxRetry, 1);
                } else if (instanceWaitObject.isFailedCheck()) {
                    client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceFailedChecker<>(), instanceWaitObject, testContext,
                            pollingInterval, maxRetry, 1);
                } else {
                    if (runningParameter != null && runningParameter.getTimeoutChecker() != null) {
                        client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceOperationChecker<>(), instanceWaitObject, testContext,
                                pollingInterval, runningParameter.getTimeoutChecker(), maxRetry);
                    } else {
                        client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceOperationChecker<>(), instanceWaitObject, testContext,
                                pollingInterval, maxRetry, 1);
                    }
                }
            });
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e),
                        entity.getName());
                Log.await(null, String.format("[%s] is failed for statuses %s: %s, name: %s",
                        entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName()));
            }
            testContext.getExceptionMap().put("await " + entity + " for desired statuses " + desiredStatuses, e);
        }
        return entity;
    }

    public <E extends Enum<E>> CloudbreakTestDto awaitExistence(CloudbreakTestDto entity, TestContext testContext, RunningParameter runningParameter) {
        if (entity == null) {
            throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
        }
        try {
            Log.await(LOGGER, String.format("%s for instance existence", entity.getName()));
            Duration pollingInterval = testContext.getPollingDurationOrTheDefault(runningParameter);
            int maxRetry = testContext.getMaxRetry();
            MicroserviceClient client = testContext.getMicroserviceClient(entity.getClass(), testContext.setActingUser(runningParameter).getAccessKey());

            InstanceWaitObject instanceWaitObject = client.waitInstancesObject(entity, testContext, List.of(), null);

            if (runningParameter.getTimeoutChecker() != null) {
                client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceExistenceChecker<>(), instanceWaitObject, testContext,
                        pollingInterval, runningParameter.getTimeoutChecker(), maxRetry);
            } else {
                client.<CloudbreakInstanceWaitObject>waiterService().waitObject(new InstanceExistenceChecker<>(), instanceWaitObject, testContext,
                        pollingInterval, maxRetry, 1);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] for instance existence is failed due to: {}, name: {}", entity, ResponseUtil.getErrorMessage(e),
                        entity.getName());
                Log.await(null, String.format("[%s] is failed for instance existence due to: %s, name: %s",
                        entity, ResponseUtil.getErrorMessage(e), entity.getName()));
            }
            testContext.getExceptionMap().put("await " + entity + " for instance existence", e);
        }
        return entity;
    }
}