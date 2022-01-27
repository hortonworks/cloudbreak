package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Component
public class ResourceAwait {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAwait.class);

    public <E extends Enum<E>> CloudbreakTestDto await(CloudbreakTestDto entity, Map<String, E> desiredStatuses,
            TestContext testContext, RunningParameter runningParameter, Duration pollingInterval, int maxRetry, int maxRetryCount, MicroserviceClient client) {
        try {
            if (entity == null) {
                throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            String name = entity.getName();
            WaitObject waitObject = client.waitObject(entity, name, desiredStatuses, testContext, runningParameter.getIgnoredStatuses());
            if (waitObject.isDeletionCheck()) {
                client.waiterService().waitObject(new WaitTerminationChecker<>(), waitObject, testContext, pollingInterval, maxRetry, maxRetryCount);
            } else if (waitObject.isFailedCheck()) {
                client.waiterService().waitObject(new WaitFailedChecker<>(), waitObject, testContext, pollingInterval, maxRetry, maxRetryCount);
                entity.refresh();
            } else {
                if (runningParameter != null && runningParameter.getTimeoutChecker() != null) {
                    client.waiterService().waitObject(new WaitOperationChecker<>(), waitObject, testContext, pollingInterval,
                            runningParameter.getTimeoutChecker(), maxRetryCount);
                } else {
                    client.waiterService().waitObject(new WaitOperationChecker<>(), waitObject, testContext, pollingInterval, maxRetry, maxRetryCount);
                }
                entity.refresh();
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
                Log.await(null, String.format("[%s] is failed for statuses %s: %s, name: %s",
                        entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName()));
            }
            testContext.getExceptionMap().put(entity.getAwaitExceptionKey(desiredStatuses), e);
        }
        return entity;
    }
}
