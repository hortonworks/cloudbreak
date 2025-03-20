package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackSkuMigrationAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackSkuMigrationAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "SKU migration for name: " + testDto.getName());
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        FlowIdentifier flowIdentifier = client.getDefaultClient().stackV4Endpoint()
                .triggerSkuMigration(client.getWorkspaceId(), testDto.getName(), true, initiatorUserCrn);
        testDto.setFlow("SKU migration for " + testDto.getName(), flowIdentifier);
        Log.when(LOGGER, "SKU migration started with flow id: " + flowIdentifier);
        return testDto;
    }
}
