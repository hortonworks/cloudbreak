package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackRefreshEntitlementParamInternalAction implements Action<StackTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackRefreshEntitlementParamInternalAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, String.format(" Stack refresh entitlement params requested: %s", testDto.getRequest().getName()));
        testDto.setResponse(
                client.getDefaultClient(testContext).stackV4Endpoint().get(client.getWorkspaceId(), testDto.getName(), Collections.emptySet(),
                        testContext.getActingUserCrn().getAccountId())
        );
        FlowIdentifier flowIdentifier = client.getInternalClientWithoutChecks(testContext).stackV4Endpoint()
                .refreshEntitlementParams(client.getWorkspaceId(), testDto.getCrn(), null);
        testDto.setFlow("Stack refresh entitlement params", flowIdentifier);
        Log.when(LOGGER, " Stack refresh entitlement params requested successfully");
        return testDto;
    }
}
