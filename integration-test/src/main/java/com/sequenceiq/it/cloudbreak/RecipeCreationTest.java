package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RecipeCreationTest extends AbstractCloudbreakIntegrationTest {

    private ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    @Parameters({ "name", "description", "preScript", "postScript" })
    public void testRecipeCreation(String name, @Optional String description, @Optional("") String preScript, @Optional("") String postScript) throws Exception {
        // GIVEN
        String tomlContent = String.format("[plugin]\nname=\"%s\"\ndescription=\"%s\"\nversion=\"1.0\"\n", name, "");
        StringBuilder pluginContentBuilder = new StringBuilder()
                .append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.getBytes())).append("\n");
        if (!preScript.isEmpty()) {
            addScriptContent(pluginContentBuilder, "recipe-pre-install", preScript);
        }
        if (!postScript.isEmpty()) {
            addScriptContent(pluginContentBuilder, "recipe-post-install", postScript);
        }

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("name", name);
        recipe.put("description", description);
        Map<String, String> plugins = new HashMap<>();
        plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().getBytes()), "ALL_NODES");
        recipe.put("plugins", plugins);

        // WHEN
        CloudbreakClient client = getClient();
        String id = client.postRecipe(jsonMapper.writeValueAsString(recipe), false);

        addRecipeToContext(Long.valueOf(id));
    }

    private void addRecipeToContext(Long id) {
        IntegrationTestContext itContext = getItContext();
        Set<Long> recipeIds = itContext.getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
        recipeIds = recipeIds == null ? new HashSet<Long>() : recipeIds;
        recipeIds.add(id);
        itContext.putContextParam(CloudbreakITContextConstants.RECIPE_ID, recipeIds);
    }

    private void addScriptContent(StringBuilder builder, String name, String script) {
        builder.append(name).append(":").append(Base64.encodeBase64String((script + "\n").getBytes())).append("\n");
    }
}
