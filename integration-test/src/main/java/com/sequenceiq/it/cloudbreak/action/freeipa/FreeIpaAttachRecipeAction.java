package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaAttachRecipeAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAttachRecipeAction.class);

    private final List<String> recipes;

    public FreeIpaAttachRecipeAction(List<String> recipes) {
        this.recipes = recipes;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA Environment CRN: %s", testDto.getRequest().getEnvironmentCrn()));
        RecipeAttachDetachRequest recipeAttach = new RecipeAttachDetachRequest();
        recipeAttach.setRecipes(recipes);
        recipeAttach.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        Log.whenJson(LOGGER, format(" FreeIPA recipe attach request: %n"), recipeAttach);
        client.getDefaultClient()
                .getRecipeV1Endpoint()
                .attachRecipes(recipeAttach);
        return testDto;
    }
}
