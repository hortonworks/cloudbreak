package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class SdxInternalInstanceAwait implements Await<SdxInternalTestDto, InstanceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInternalInstanceAwait.class);

    @Override
    public SdxInternalTestDto await(SdxInternalTestDto entity, Map<String, InstanceStatus> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Sdx key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            String name = entity.getName();

            desiredStatuses.forEach((hostGroup, instanceStatus) -> {
                InstanceWaitObject waitObject = new InstanceWaitObject(testContext, name, hostGroup, instanceStatus);

                if (waitObject.isDeleted(instanceStatus)) {
                    waitForSdxInternalInstanceStatus(new InstanceTerminationChecker<>(), testContext, waitObject, pollingInterval, maxRetry);
                } else if (waitObject.isFailed(instanceStatus)) {
                    waitForSdxInternalInstanceStatus(new InstanceFailedChecker<>(), testContext, waitObject, pollingInterval, maxRetry);
                } else {
                    waitForSdxInternalInstanceStatus(new InstanceOperationChecker<>(), testContext, waitObject, pollingInterval, maxRetry);
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

    private Result<WaitResult, Exception> waitForSdxInternalInstanceStatus(ExceptionChecker<InstanceWaitObject> statusChecker, TestContext testContext,
            InstanceWaitObject waitObject, Duration pollingInterval, int maxRetry) {
        return testContext.getInstanceWaitService().waitObject(statusChecker, waitObject, testContext, pollingInterval, maxRetry, 1);
    }
}