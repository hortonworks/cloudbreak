package com.sequenceiq.it.cloudbreak.action.freeipa;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;

public abstract class AbstractFreeIpaAction<U extends AbstractFreeIpaTestDto> implements Action<U, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreeIpaAction.class);

    @Override
    public U action(TestContext testContext, U testDto, FreeIpaClient client) throws Exception {
        int retries = 0;
        while (retries <= testDto.getFlowUtil().getMaxRetry()) {
            try {
                return freeIpaAction(testContext, testDto, client);
            } catch (InternalServerErrorException e) {
                String message = e.getResponse().readEntity(String.class);
                LOGGER.info("Exception during executing FreeIPA action: ", e);
                if (message.contains("Flows under operation")) {
                    waitTillFlowInOperation(testDto.getFlowUtil());
                    retries++;
                } else {
                    throw e;
                }
            } catch (ClientErrorException e) {
                LOGGER.info("Exception during executing FreeIPA action: ", e);
                Response response = e.getResponse();
                if (CONFLICT.getStatusCode() == response.getStatus()) {
                    waitTillFlowInOperation(testDto.getFlowUtil());
                    retries++;
                } else {
                    throw e;
                }
            }
        }
        throw new Exception("Exception during executing FreeIPA action: exceeding maxretry during waiting for flow");
    }

    protected abstract U freeIpaAction(TestContext testContext, U testDto, FreeIpaClient client) throws Exception;

    private void waitTillFlowInOperation(FlowUtil flowUtil) {
        try {
            Thread.sleep(flowUtil.getPollingInterval());
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait for flow to end: ", e);
        }
    }
}
