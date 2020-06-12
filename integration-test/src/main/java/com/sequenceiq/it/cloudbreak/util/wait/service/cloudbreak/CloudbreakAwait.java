package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.Await;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.it.cloudbreak.util.wait.service.Result;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitResult;

@Component
public class CloudbreakAwait implements Await<CloudbreakTestDto, Status> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakAwait.class);

    private static final Map<String, Status> STACK_DELETED = Map.of("status", Status.DELETE_COMPLETED);

    private static final Map<String, Status> STACK_FAILED = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    @Override
    public CloudbreakTestDto await(CloudbreakTestDto entity, Map<String, Status> desiredStatuses, TestContext testContext,
            RunningParameter runningParameter, Duration pollingInterval, int maxRetry) {
        try {
            if (entity == null) {
                throw new RuntimeException("Cloudbreak key has been provided but no result in resource map!");
            }
            Log.await(LOGGER, String.format("%s for %s", entity.getName(), desiredStatuses));
            CloudbreakClient client = testContext.getMicroserviceClient(CloudbreakClient.class, testContext.getWho(runningParameter)
                    .getAccessKey());
            String name = entity.getName();
            if (desiredStatuses.equals(STACK_DELETED)) {
                waitForCloudbreakStatuses(new CloudbreakTerminationChecker<>(), client, name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
            } else if (desiredStatuses.equals(STACK_FAILED)) {
                waitForCloudbreakStatuses(new CloudbreakFailedChecker<>(), client, name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
                entity.refresh(testContext, client);
            } else {
                waitForCloudbreakStatuses(new CloudbreakOperationChecker<>(), client, name, testContext, desiredStatuses,
                        pollingInterval, maxRetry);
                entity.refresh(testContext, client);
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

    private Result<WaitResult, Exception> waitForCloudbreakStatuses(ExceptionChecker<CloudbreakWaitObject> statusChecker, CloudbreakClient client,
            String name, TestContext testContext, Map<String, Status> desiredStatuses, Duration pollingInterval, int maxRetry) {
        return testContext.getCloudbreakWaitService().waitObject(statusChecker, new CloudbreakWaitObject(client, name, desiredStatuses), testContext,
                pollingInterval, maxRetry, 1);
    }
}
