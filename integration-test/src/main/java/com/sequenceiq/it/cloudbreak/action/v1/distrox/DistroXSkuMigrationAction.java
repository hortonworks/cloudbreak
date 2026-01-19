package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXSkuMigrationAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXSkuMigrationAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "SKU migration for name: " + testDto.getName());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).distroXV1Endpoint()
                .triggerSkuMigrationByName(testDto.getName(), true);
        testDto.setFlow("SKU migration for " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "SKU migration started with flow id: " + flowIdentifier);
        return testDto;
    }
}
