package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.instance;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Component
public class FreeIpaInstanceAwait {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceAwait.class);

    public FreeIpaTestDto await(FreeIpaTestDto entity, Map<List<String>, InstanceStatus> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Sdx key has been provided but no result in resource map!");
            }
            String envCrn = entity.getResponse().getEnvironmentCrn();
            Log.await(LOGGER, String.format("%s for %s", envCrn, desiredStatuses));
            MicroserviceClient client = testContext.getMicroserviceClient(entity.getClass(), testContext.setActingUser(runningParameter).getAccessKey());

            desiredStatuses.forEach((instanceIds, instanceStatus) -> {
                FreeIpaInstanceWaitObject waitObject = new FreeIpaInstanceWaitObject(testContext, envCrn, instanceIds, instanceStatus);

                if (waitObject.isDeletionCheck()) {
                    client.<FreeIpaInstanceWaitObject>waiterService().waitObject(new FreeIpaInstanceTerminationChecker<>(), waitObject, testContext,
                            pollingInterval,
                            maxRetry, 1);
                } else if (waitObject.isFailedCheck()) {
                    client.<FreeIpaInstanceWaitObject>waiterService().waitObject(new FreeIpaInstanceFailedChecker<>(), waitObject, testContext, pollingInterval,
                            maxRetry, 1);
                } else {
                    client.<FreeIpaInstanceWaitObject>waiterService().waitObject(new FreeIpaInstanceOperationChecker<>(), waitObject, testContext,
                            pollingInterval, maxRetry, 1);
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
}