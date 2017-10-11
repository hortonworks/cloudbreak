package com.sequenceiq.it.cloudbreak.recipes;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.springframework.util.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.common.type.RecipeType;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class RecipeCreationTest extends AbstractCloudbreakIntegrationTest {

    @Test
    @Parameters({ "name", "description", "preScript", "postScript" })
    public void testRecipeCreation(String name, @Optional ("") String description, @Optional("") String preScript,
            @Optional("") String postScript) throws Exception {
        // GIVEN
        // WHEN
        if (!preScript.isEmpty()) {
            createRecipe(name + "pre", preScript, RecipeType.PRE, description);
        }
        if (!postScript.isEmpty()) {
            createRecipe(name + "post", postScript, RecipeType.POST, description);
        }
    }

    private void addRecipeToContext(Long id) {
        IntegrationTestContext itContext = getItContext();
        Set<Long> recipeIds = itContext.getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
        recipeIds = recipeIds == null ? new HashSet<>() : recipeIds;
        recipeIds.add(id);
        itContext.putContextParam(CloudbreakITContextConstants.RECIPE_ID, recipeIds);
    }

    private void createRecipe(String name, String script, RecipeType recipeType, String description) {
        RecipeRequest recipeRequest = new RecipeRequest();
        recipeRequest.setRecipeType(recipeType);
        recipeRequest.setName(name);
        recipeRequest.setContent(Base64.encodeBase64String(script.getBytes()));
        recipeRequest.setDescription(description);

        RecipeEndpoint recipeEndpoint = getCloudbreakClient().recipeEndpoint();
        Long id = recipeEndpoint.postPrivate(recipeRequest).getId();
        //then
        Assert.notNull(id, "Recipe is not created.");
        addRecipeToContext(id);
    }
}