package com.sequenceiq.it.cloudbreak.action.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxSkuMigrationAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxSkuMigrationAction.class);

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, "SKU migration for name: " + testDto.getName());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().triggerSkuMigrationByName(testDto.getName(), true);
        testDto.setFlow("SKU migration for " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "SKU migration started with flow id: " + flowIdentifier);
        return testDto;
    }
}
