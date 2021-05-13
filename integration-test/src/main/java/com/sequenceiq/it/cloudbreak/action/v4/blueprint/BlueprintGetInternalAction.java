package com.sequenceiq.it.cloudbreak.action.v4.blueprint;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class BlueprintGetInternalAction implements Action<BlueprintTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintGetInternalAction.class);

    @Override
    public BlueprintTestDto action(TestContext testContext, BlueprintTestDto testDto, CloudbreakClient client) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testDto.getAccountId()), "Account id must be set to get blueprint by name on internal API.");
        Log.whenJson(LOGGER, format(" Blueprint getByName request: %n"), testDto.getName());
        testDto.setResponse(
                client.getInternalClient(testContext)
                        .blueprintV4Endpoint()
                        .getByNameInternal(client.getWorkspaceId(), testDto.getAccountId(), testDto.getName()));
        Log.whenJson(LOGGER, format(" Blueprint getByName successfully:%n"), testDto.getResponse());

        return testDto;
    }

}