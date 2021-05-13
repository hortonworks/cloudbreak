package com.sequenceiq.it.cloudbreak.action.v4.recipe;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RecipeGetInternalAction implements Action<RecipeTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeGetInternalAction.class);

    @Override
    public RecipeTestDto action(TestContext testContext, RecipeTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(testDto.getAccountId()), "Account id must be set to get recipe by name on internal API.");
        testDto.setResponse(
                cloudbreakClient.getInternalClient(testContext)
                        .recipeV4Endpoint().getByNameInternal(cloudbreakClient.getWorkspaceId(), testDto.getAccountId(), testDto.getName())
        );
        Log.whenJson(LOGGER, format(" Recipe get by name internal successfully:%n"), testDto.getResponse());

        return testDto;
    }
}