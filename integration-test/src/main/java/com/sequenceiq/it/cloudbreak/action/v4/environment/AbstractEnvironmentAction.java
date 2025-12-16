package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public abstract class AbstractEnvironmentAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnvironmentAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        int retries = 0;
        while (retries <= testContext.getMaxRetry()) {
            try {
                return environmentAction(testContext, testDto, client);
            } catch (Exception e) {
                if (e.getMessage().contains("flow under operation")) {
                    waitTillFlowInOperation(testContext);
                    retries++;
                } else {
                    throw e;
                }
            }
        }
        throw new Exception("Exception during executing Environment action: exceeding maxretry during waiting for flow");
    }

    protected abstract EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception;

    private void waitTillFlowInOperation(TestContext testContext) {
        try {
            Thread.sleep(testContext.getPollingInterval());
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait for a flow to end: ", e);
        }
    }
}