package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class DistroxInstanceAwait implements Await<DistroXTestDto, InstanceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxInstanceAwait.class);

    private static final Map<String, InstanceStatus> DELETED_ON_PROVIDER_SIDE = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        put(IDBROKER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
    }};

    private static final Map<String, InstanceStatus> ORCHESTRATION_FAILED = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.ORCHESTRATION_FAILED);
        put(IDBROKER.getName(), InstanceStatus.ORCHESTRATION_FAILED);
    }};

    @Override
    public DistroXTestDto await(DistroXTestDto entity, Map<String, InstanceStatus> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Distrox key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            String name = entity.getName();
            if (desiredStatuses.equals(DELETED_ON_PROVIDER_SIDE)) {
                waitForDistroxInstanceStatuses(new InstanceTerminationChecker<>(), name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            } else if (desiredStatuses.equals(ORCHESTRATION_FAILED)) {
                waitForDistroxInstanceStatuses(new InstanceFailedChecker<>(), name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            } else {
                waitForDistroxInstanceStatuses(new InstanceOperationChecker<>(), name, testContext, desiredStatuses,
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

    private Result<WaitResult, Exception> waitForDistroxInstanceStatuses(ExceptionChecker<InstanceWaitObject> statusChecker,
            String name, TestContext testContext, Map<String, InstanceStatus> desiredStatuses, Duration pollingInterval, int maxRetry) {
        return testContext.getInstanceWaitService().waitObject(statusChecker, new InstanceWaitObject(testContext, name, desiredStatuses),
                testContext, pollingInterval, maxRetry, 1);
    }
}