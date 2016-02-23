package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderObjectMap;
import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.PluginExecutionType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class RecipeCommands implements CommandMarker {

    @Inject
    private CloudbreakContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private ExceptionTransformer exceptionTransformer;

    @CliAvailabilityIndicator(value = "recipe list")
    public boolean isRecipeListCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "recipe select")
    public boolean isRecipeSelectCommandAvailable() throws Exception {
        return context.isRecipeAccessible() && !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "recipe add")
    public boolean isRecipeAddCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "recipe show")
    public boolean isRecipeShowCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "recipe delete")
    public boolean isRecipeDeleteCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliCommand(value = "recipe list", help = "Shows the currently available recipes")
    public String listRecipes() {
        try {
            return renderSingleMap(responseTransformer.transformToMap(cloudbreakClient.recipeEndpoint().getPublics(), "id", "name"), true, "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe add", help = "Add a new recipe with either --url or --file")
    public String addRecipe(
            @CliOption(key = "url", mandatory = false, help = "URL of the Recipe to download from") String url,
            @CliOption(key = "file", mandatory = false, help = "File which contains the Recipe") File file,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the recipe is public in the account") Boolean publicInAccount) {
        try {
            String json = file == null ? IOUtils.toString(new URL(url)) : IOUtils.toString(new FileInputStream(file));
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            IdJson id;
            RecipeRequest recipeRequest = mapper.readValue(json, RecipeRequest.class);
            if (publicInAccount) {
                id = cloudbreakClient.recipeEndpoint().postPublic(recipeRequest);
            } else {
                id = cloudbreakClient.recipeEndpoint().postPrivate(recipeRequest);
            }
            return String.format("Recipe '%s' has been added with id: %s", recipeRequest.getName(), id.getId());
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe show", help = "Shows the properties of the specified recipe")
    public Object showRecipe(
            @CliOption(key = "id", mandatory = false, help = "Id of the recipe") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the recipe") String name) {
        try {
            RecipeResponse recipeMap = null;
            if (id != null) {
                recipeMap = cloudbreakClient.recipeEndpoint().get(Long.valueOf(id));
            } else if (name != null) {
                recipeMap = cloudbreakClient.recipeEndpoint().getPublic(name);
            } else {
                return "Recipe not specified.";
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", recipeMap.getId().toString());
            map.put("name", recipeMap.getName());
            map.put("description", recipeMap.getDescription());
            map.put("timeout", recipeMap.getTimeout().toString());

            return renderSingleMap(map, "FIELD", "INFO") + "\n\n"
                    + renderSingleMap(recipeMap.getProperties(), "CONSUL-KEY", "VALUE") + "\n\n"
                    + renderObjectMap(recipeMap.getPlugins(), "PLUGIN", "EXECUTION_TYPE") + "\n\n";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe delete", help = "Delete the recipe by its id or name")
    public Object deleteRecipe(
            @CliOption(key = "id", mandatory = false, help = "Id of the recipe") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the recipe") String name) {
        try {
            if (id != null) {
                cloudbreakClient.recipeEndpoint().delete(Long.valueOf(id));
                return String.format("Recipe deleted with %s id", id);
            } else if (name != null) {
                cloudbreakClient.recipeEndpoint().deletePublic(name);
                return String.format("Recipe deleted with %s name", name);
            }
            return "Recipe not specified (select recipe by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe store", help = "Create and store a new recipe")
    public String storeRecipe(
            @CliOption(key = "name", mandatory = true, help = "Unique name of the recepie") String name,
            @CliOption(key = "description", help = "Description of the recepie") String description,
            @CliOption(key = "executionType", mandatory = true, help = "Type of recepie execution") PluginExecutionType executionType,
            @CliOption(key = "preInstallScriptFile", help = "Path of the pre install script file") File preInstallScriptFile,
            @CliOption(key = "postInstallScriptFile", help = "Path of the post install script file") File postInstallScriptFile,
            @CliOption(key = "timeout", help = "Timeout of the script execution") Integer timeout,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the recipe is public in the account") Boolean publicInAccount) {
        if (preInstallScriptFile != null && !preInstallScriptFile.exists()) {
            return "Pre install script file not exists.";
        } else if (postInstallScriptFile != null && !postInstallScriptFile.exists()) {
            return "Post install script file not exists.";
        } else if (preInstallScriptFile == null && postInstallScriptFile == null) {
            return "At least one script is required.";
        }
        try {
            String tomlContent = String.format("[plugin]\nname=\"%s\"\ndescription=\"%s\"\nversion=\"1.0\"\n", name, description == null ? "" : description);
            StringBuilder pluginContentBuilder = new StringBuilder()
                    .append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.getBytes())).append("\n");
            if (preInstallScriptFile != null) {
                addScriptContent(pluginContentBuilder, "recipe-pre-install", preInstallScriptFile);
            }
            if (postInstallScriptFile != null) {
                addScriptContent(pluginContentBuilder, "recipe-post-install", postInstallScriptFile);
            }

            Map<String, com.sequenceiq.cloudbreak.api.model.PluginExecutionType> plugins = new HashMap<>();
            plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().getBytes()),
                    com.sequenceiq.cloudbreak.api.model.PluginExecutionType.valueOf(executionType.getName()));

            RecipeRequest recipeRequest = new RecipeRequest();
            recipeRequest.setName(name);
            recipeRequest.setDescription(description);
            recipeRequest.setTimeout(timeout);
            recipeRequest.setPlugins(plugins);
            IdJson id;
            if (publicInAccount) {
                id = cloudbreakClient.recipeEndpoint().postPublic(recipeRequest);
            } else {
                id = cloudbreakClient.recipeEndpoint().postPrivate(recipeRequest);
            }
            return String.format("Recipe '%s' has been stored with id: %s", name, id.getId());
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    private void addScriptContent(StringBuilder builder, String name, File scriptFile) throws IOException {
        String script = IOUtils.toString(new FileInputStream(scriptFile));
        builder.append(name).append(":").append(Base64.encodeBase64String(script.getBytes())).append("\n");
    }
}
