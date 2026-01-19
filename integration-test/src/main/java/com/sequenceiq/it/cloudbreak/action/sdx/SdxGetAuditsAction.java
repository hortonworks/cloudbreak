package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxGetAuditsAction implements Action<SdxEventTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxGetAuditsAction.class);

    @Override
    public SdxEventTestDto action(TestContext testContext, SdxEventTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, "Getting audit events via " + client.getDefaultClient(testContext).sdxEventEndpoint() + ", for input " + testDto.argsToString());
        List<CDPStructuredEvent> auditEvents = client.getDefaultClient(testContext).sdxEventEndpoint().getAuditEvents(
                testDto.getEnvironmentCrn(), testDto.getTypes(), testDto.getPage(), testDto.getSize()
        );
        Log.when(LOGGER, "Audit events response: " + auditEvents);
        return testDto;
    }
}
