package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDetachRecipeAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDetachRecipeAction.class);

    private final List<String> recipes;

    public FreeIpaDetachRecipeAction(List<String> recipes) {
        this.recipes = recipes;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        Log.when(LOGGER, format(" FreeIPA Environment CRN: %s", environmentCrn));
        RecipeAttachDetachRequest recipeDetach = new RecipeAttachDetachRequest();
        recipeDetach.setRecipes(recipes);
        recipeDetach.setEnvironmentCrn(environmentCrn);
        Log.whenJson(LOGGER, format(" FreeIPA recipe detach request: %n"), recipeDetach);
        client.getDefaultClient(testContext)
                .getRecipeV1Endpoint()
                .detachRecipes(recipeDetach);
        return testDto;
    }
}
