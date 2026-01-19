package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXUpdatePublicDnsEntriesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpdatePublicDnsEntriesAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "Update public dns entries by name: " + testDto.getName());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).distroXV1Endpoint().updatePublicDnsEntriesByName(testDto.getName());
        testDto.setFlow("Update public dns entries started by name: " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "Public dns entries update started with flow id: " + flowIdentifier);
        return testDto;
    }
}
