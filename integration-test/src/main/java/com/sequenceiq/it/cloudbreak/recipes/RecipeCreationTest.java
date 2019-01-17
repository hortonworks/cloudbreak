package com.sequenceiq.it.cloudbreak.recipes;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class RecipeCreationTest extends AbstractCloudbreakIntegrationTest {

    @Test
    @Parameters({ "name", "description", "preScript", "postScript" })
    public void testRecipeCreation(String name, @Optional ("") String description, @Optional("") String preScript,
            @Optional("") String postScript) {
        // GIVEN
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        // WHEN
        if (!preScript.isEmpty()) {
            createRecipe(name + "pre", preScript, RecipeV4Type.POST_CLUSTER_MANAGER_START, description, workspaceId);
        }
        if (!postScript.isEmpty()) {
            createRecipe(name + "post", postScript, RecipeV4Type.POST_CLUSTER_INSTALL, description, workspaceId);
        }
    }

    private void addRecipeToContext(Long id) {
        IntegrationTestContext itContext = getItContext();
        Set<Long> recipeIds = itContext.getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
        recipeIds = recipeIds == null ? new HashSet<>() : recipeIds;
        recipeIds.add(id);
        itContext.putContextParam(CloudbreakITContextConstants.RECIPE_ID, recipeIds);
    }

    private void createRecipe(String name, String script, RecipeV4Type recipeType, String description, Long workspaceId) {
        RecipeV4Request recipeRequest = new RecipeV4Request();
        recipeRequest.setType(recipeType);
        recipeRequest.setName(name);
        recipeRequest.setContent(Base64.encodeBase64String(script.getBytes()));
        recipeRequest.setDescription(description);

        RecipeV4Endpoint recipeEndpoint = getCloudbreakClient().recipeV4Endpoint();
        Long id = recipeEndpoint.post(workspaceId, recipeRequest).getId();
        //then
        Assert.assertNotNull(id, "Recipe is not created.");
        addRecipeToContext(id);
    }
}