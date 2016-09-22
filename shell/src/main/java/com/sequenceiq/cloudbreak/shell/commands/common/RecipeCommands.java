package com.sequenceiq.cloudbreak.shell.commands.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class RecipeCommands implements BaseCommands {

    private static final String CREATE_SUCCESS_MESSAGE = "Recipe created with id: %s and name: '%s'";

    private ShellContext shellContext;

    public RecipeCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "recipe list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "recipe list", help = "Shows the currently available recipes")
    @Override
    public String list() {
        try {
            Set<RecipeResponse> publics = shellContext.cloudbreakClient().recipeEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public boolean selectAvailable() {
        return false;
    }

    @Override
    public String select(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Recipe select command not supported.");
    }

    @Override
    public String selectById(Long id) throws Exception {
        return select(id, null);
    }

    @Override
    public String selectByName(String name) throws Exception {
        return select(null, name);
    }

    @CliAvailabilityIndicator("recipe create")
    public boolean createAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "recipe create", help = "Creates a new recipe")
    public String createRecipe(
            @CliOption(key = "name", mandatory = true, help = "Unique name of the recepie") String name,
            @CliOption(key = "description", help = "Description of the recepie") String description,
            @CliOption(key = "preInstallScriptFile", help = "Path of the pre install script file") File preInstallScriptFile,
            @CliOption(key = "postInstallScriptFile", help = "Path of the post install script file") File postInstallScriptFile,
            @CliOption(key = "timeout", help = "Timeout of the script execution") Integer timeout,
            @CliOption(key = "publicInAccount", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the recipe is public in the account") Boolean publicInAccount) {
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

            Set<String> plugins = new HashSet<>();
            plugins.add("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().getBytes()));

            RecipeRequest recipeRequest = new RecipeRequest();
            recipeRequest.setName(name);
            recipeRequest.setDescription(description);
            recipeRequest.setTimeout(timeout);
            recipeRequest.setPlugins(plugins);
            IdJson id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPublic(recipeRequest);
            } else {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPrivate(recipeRequest);
            }
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), recipeRequest.getName());
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "recipe show --id", "recipe show --name" })
    @Override
    public boolean showAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "recipe show", help = "Shows the properties of the specified recipe")
    @Override
    public String show(Long id, String name) {
        try {
            RecipeResponse recipeMap;
            if (id != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint().get(id);
            } else if (name != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint().getPublic(name);
            } else {
                return "Recipe not specified.";
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", recipeMap.getId().toString());
            map.put("name", recipeMap.getName());
            map.put("description", recipeMap.getDescription());
            map.put("timeout", recipeMap.getTimeout().toString());

            return shellContext.outputTransformer().render(map, "FIELD", "INFO") + "\n\n"
                    + shellContext.outputTransformer().render(recipeMap.getProperties(), "CONSUL-KEY", "VALUE") + "\n\n"
                    + shellContext.outputTransformer().render(recipeMap.getPlugins(), "PLUGIN", "EXECUTION_TYPE") + "\n\n";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe show --id", help = "Show the recipe by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "recipe show --name", help = "Show the recipe by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @CliAvailabilityIndicator(value = { "recipe delete --id", "recipe delete --name" })
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "recipe delete", help = "Delete the recipe by its id or name")
    @Override
    public String delete(Long id, String name) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().recipeEndpoint().delete(id);
                return String.format("Recipe deleted with %s id", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().recipeEndpoint().deletePublic(name);
                return String.format("Recipe deleted with %s name", name);
            }
            return "Recipe not specified (select recipe by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe delete --id", help = "Delete the recipe by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "recipe delete --name", help = "Delete the recipe by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private void addScriptContent(StringBuilder builder, String name, File scriptFile) throws IOException {
        String script = IOUtils.toString(new FileInputStream(scriptFile));
        builder.append(name).append(":").append(Base64.encodeBase64String(script.getBytes())).append("\n");
    }

}
