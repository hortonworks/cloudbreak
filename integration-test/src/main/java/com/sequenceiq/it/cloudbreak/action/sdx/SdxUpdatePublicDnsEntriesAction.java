package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxUpdatePublicDnsEntriesAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpdatePublicDnsEntriesAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, "Update public dns entries by name: " + testDto.getName());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().updatePublicDnsEntriesByName(testDto.getName());
        testDto.setFlow("Update public dns entries started by name: " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "Public dns entries update started with flow id: " + flowIdentifier);
        return testDto;
    }
}
