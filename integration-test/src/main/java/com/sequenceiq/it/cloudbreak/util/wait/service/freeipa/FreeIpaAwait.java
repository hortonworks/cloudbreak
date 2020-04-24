package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class FreeIpaAwait implements Await<FreeIPATestDto, Status> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAwait.class);

    @Override
    public FreeIPATestDto await(FreeIPATestDto entity, Status desiredStatus, TestContext testContext, RunningParameter runningParameter,
            long pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("FreeIpa key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getResponse().getName(), desiredStatus));
            FreeIPAClient client = testContext.getMicroserviceClient(FreeIPAClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String environmentCrn = entity.getResponse().getEnvironmentCrn();
            switch (desiredStatus) {
                case AVAILABLE:
                case STOPPED:
                    waitForFreeIpaStatus(new FreeIpaOperationChecker<>(), client, environmentCrn, testContext, desiredStatus,
                            pollingInterval, maxRetry);
                    break;
                case DELETE_COMPLETED:
                    waitForFreeIpaStatus(new FreeIpaTerminationChecker<>(), client, environmentCrn, testContext, desiredStatus,
                            pollingInterval, maxRetry);
                    break;
                default:
                    LOGGER.warn("Wait checker is not implemented yet for the desired freeIpa state '{}' ", desiredStatus);
                    break;
            }
            testContext.setStatuses(Map.of("status", entity.getResponse().getStatus().name()));
            if (!desiredStatus.equals(DELETE_COMPLETED)) {
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

    private Result<WaitResult, Exception> waitForFreeIpaStatus(ExceptionChecker<FreeIpaWaitObject> statusChecker, FreeIPAClient client,
            String environmentCrn, TestContext testContext, Status desiredStatus, long pollingInterval, int maxRetry) {
        return testContext.getFreeIpaWaitService().waitWithTimeout(
                statusChecker,
                new FreeIpaWaitObject(client, environmentCrn, desiredStatus),
                pollingInterval, maxRetry, 1);
    }
}
