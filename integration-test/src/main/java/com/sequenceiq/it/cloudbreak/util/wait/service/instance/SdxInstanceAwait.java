package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class SdxInstanceAwait implements Await<SdxTestDto, InstanceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInstanceAwait.class);

    @Override
    public SdxTestDto await(SdxTestDto entity, Map<String, InstanceStatus> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Sdx key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            String name = entity.getName();
            if (isDeleted(desiredStatuses)) {
                waitForSdxInstanceStatuses(new InstanceTerminationChecker<>(), name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            } else if (isFailed(desiredStatuses)) {
                waitForSdxInstanceStatuses(new InstanceFailedChecker<>(), name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            } else {
                waitForSdxInstanceStatuses(new InstanceOperationChecker<>(), name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            }
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

    private Result<WaitResult, Exception> waitForSdxInstanceStatuses(ExceptionChecker<InstanceWaitObject> statusChecker,
            String name, TestContext testContext, Map<String, InstanceStatus> desiredStatuses, Duration pollingInterval, int maxRetry) {
        return testContext.getInstanceWaitService().waitObject(statusChecker, new InstanceWaitObject(testContext, name, desiredStatuses),
                testContext, pollingInterval, maxRetry, 1);
    }

    private boolean isFailed(Map<String, InstanceStatus> instanceStatuses) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, ORCHESTRATION_FAILED, DECOMMISSION_FAILED);
        return !Sets.intersection(Set.of(instanceStatuses.values()), failedStatuses).isEmpty();
    }

    private boolean isDeleted(Map<String, InstanceStatus> instanceStatuses) {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return !Sets.intersection(Set.of(instanceStatuses.values()), deletedStatuses).isEmpty();
    }
}
