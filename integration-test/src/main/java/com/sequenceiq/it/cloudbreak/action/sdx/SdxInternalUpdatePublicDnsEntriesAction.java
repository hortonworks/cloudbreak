package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxInternalUpdatePublicDnsEntriesAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInternalUpdatePublicDnsEntriesAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, "Update public dns entries by name: " + testDto.getName());
        FlowIdentifier flowIdentifier = client.getDefaultClient().sdxEndpoint().updatePublicDnsEntriesByName(testDto.getName());
        testDto.setFlow("Update public dns entries started by name: " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "Public dns entries update started with flow id: " + flowIdentifier);
        return testDto;
    }
}
