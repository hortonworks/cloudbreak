package com.sequenceiq.it.cloudbreak.action.v4.stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class StackAttachRecipeAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackAttachRecipeAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, " attach recipe to stack request:\n", testDto.getAttachRecipeV4Request());
        AttachRecipeV4Response response = client.getDefaultClient().stackV4Endpoint().attachRecipe(
                client.getWorkspaceId(),
                testDto.getAttachRecipeV4Request(),
                testDto.getName(),
                testContext.getActingUserCrn().getAccountId());
        Log.whenJson(LOGGER, " attach recipe to stack response:\n", response);
        return testDto;
    }
}
