package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Recipe;

public class RecipeTests extends CloudbreakTest {
    private static final String VALID_RECIPE_NAME = "valid-recipe";

    private static final String VALID_RECIPE_DESCRIPTION = "recipe for API E2E tests";

    private static final String VALID_RECIPE_SCRIPT = "echo test";

    private static final String VALID_RECIPE_SCRIPT_FILE = "classpath:/recipes/valid_recipe.sh";

    private static final String INVALID_RECIPE_NAME_SHORT = "test";

    private static final String SPECIAL_RECIPE_NAME = "@#$%|:&*;";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeTests.class);

    @Test
    public void testCreateValidRecipePostAmbari() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-post-ambari")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-post-ambari"))
        );
    }

    @Test
    public void testCreateValidRecipePreAmbari() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-pre-ambari")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.PRE_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-pre-ambari"))
        );
    }

    @Test
    public void testCreateValidRecipePostCluster() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-post-cluster")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_INSTALL)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-post-cluster"))

        );
    }

    @Test
    public void testCreateValidRecipePreTerm() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-pre-term")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.PRE_TERMINATION)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-pre-term"))
        );
    }

    @Test
    public void testCreateValidRecipeFile() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-from-file")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(getRecipeFile(VALID_RECIPE_SCRIPT_FILE))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-from-file"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateSpecialNameRecipe() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(SPECIAL_RECIPE_NAME)
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(getRecipeFile(VALID_RECIPE_SCRIPT_FILE))
        );
        when(Recipe.post());
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateAgainRecipe() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.isCreated()
                .withName(VALID_RECIPE_NAME + "-again"));
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-again")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertNotNull(recipe.getResponse()))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRecipeShortName() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(INVALID_RECIPE_NAME_SHORT)
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertNotEquals(recipe.getResponse().getName(), INVALID_RECIPE_NAME_SHORT))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRecipeLongName() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(createLongString("a", 101))
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertNotEquals(recipe.getResponse().getName(), createLongString("a", 101)))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRecipeLongDescr() throws Exception {
        given(CloudbreakClient.created());
        given(Recipe.request()
                .withName(VALID_RECIPE_NAME)
                .withDescription(createLongString("a", 1001))
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertNotEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME))
        );
    }

    @Test
    public void testCreateDeleteValidCreateAgain() throws Exception {
        given(CloudbreakClient.created());

        given(Recipe.isCreatedDeleted()
                .withName(VALID_RECIPE_NAME + "-delete-create")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );

        given(Recipe.request()
                .withName(VALID_RECIPE_NAME + "-delete-create")
                .withDescription(VALID_RECIPE_DESCRIPTION)
                .withRecipeType(RecipeV4Type.POST_CLUSTER_MANAGER_START)
                .withContent(Base64.encodeBase64String(VALID_RECIPE_SCRIPT.getBytes()))
        );
        when(Recipe.post());
        then(Recipe.assertThis(
                (recipe, t) -> Assert.assertEquals(recipe.getResponse().getName(), VALID_RECIPE_NAME + "-delete-create"))
        );
    }

    @AfterSuite
    public void cleanUp() throws Exception {
        given(CloudbreakClient.created());
        when(Recipe.getAll());
        then(Recipe.assertThis(
                (recipe, t) -> {
                    Set<RecipeV4Response> recipeResponses = recipe.getResponses();
                    for (RecipeV4Response response : recipeResponses) {

                        if (response.getName().startsWith("valid")) {
                            try {
                                given(Recipe.request().withName(response.getName()));
                                when(Recipe.delete());
                            } catch (Exception e) {
                                LOGGER.error("Error occured during cleanup: " + e.getMessage());
                            }
                        }
                    }
                })
        );
    }

    private String getRecipeFile(String location) throws IOException {
        return Base64.encodeBase64String(
                StreamUtils.copyToByteArray(
                        applicationContext
                                .getResource(location)
                                .getInputStream()));
    }

    private String createLongString(String character, int length) {
        StringBuilder customLongString = new StringBuilder();
        for (int i = 0; i <= length; i++) {
            customLongString.append(character);
        }
        return customLongString.toString();
    }
}