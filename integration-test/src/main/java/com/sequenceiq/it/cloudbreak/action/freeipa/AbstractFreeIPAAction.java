package com.sequenceiq.it.cloudbreak.action.freeipa;

import javax.ws.rs.InternalServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIPATestDto;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;

public abstract class AbstractFreeIPAAction<U extends AbstractFreeIPATestDto> implements Action<U, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreeIPAAction.class);

    @Override
    public U action(TestContext testContext, U testDto, FreeIPAClient client) throws Exception {
        int retries = 0;
        while (retries <= testDto.getWaitUtil().getMaxRetry()) {
            try {
                return freeIPAAction(testContext, testDto, client);
            } catch (InternalServerErrorException e) {
                String message = e.getResponse().readEntity(String.class);
                LOGGER.info("Exception during executing FreeIPA action: ", e);
                if (message.contains("Flows under operation")) {
                    waitTillFlowInOperation(testDto.getWaitUtil());
                    retries++;
                } else {
                    throw e;
                }
            }
        }
        throw new Exception("Exception during executing FreeIPA action: exceeding maxretry during waiting for flow");
    }

    protected abstract U freeIPAAction(TestContext testContext, U testDto, FreeIPAClient client) throws Exception;

    private void waitTillFlowInOperation(WaitUtil waitUtil) {
        try {
            Thread.sleep(waitUtil.getPollingInterval());
        } catch (InterruptedException e) {
            LOGGER.warn("Exception has been occurred during wait for flow to end: ", e);
        }
    }
}
