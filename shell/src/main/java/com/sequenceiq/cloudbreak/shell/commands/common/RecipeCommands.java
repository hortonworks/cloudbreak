package com.sequenceiq.cloudbreak.shell.commands.common;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.type.RecipeType;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class RecipeCommands implements BaseCommands {

    private static final String CREATE_SUCCESS_MESSAGE = "Recipe created with id: %s and name: '%s'";

    private ShellContext shellContext;

    public RecipeCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("recipe list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
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
        throw new MethodNotSupportedException("Recipe select command not supported");
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
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "recipe create", help = "Creates a new recipe")
    public String createRecipe(
            @CliOption(key = "name", mandatory = true, help = "Unique name of the recepie") String name,
            @CliOption(key = "type", mandatory = true, help = "Type of recipe") RecipeType recipeType,
            @CliOption(key = "description", help = "Description of the recepie") String description,
            @CliOption(key = "scriptFile", help = "Path of the script file") File scriptFile,
            @CliOption(key = "scriptUrl", help = "URL for the script") String scriptUrl,
            @CliOption(key = "publicInAccount", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the recipe is public in the account") Boolean publicInAccount) {

        if (scriptFile != null && !scriptFile.exists()) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Recipe file not exists");
        } else if (scriptFile != null && scriptUrl != null) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Either the file or the url can be provided for recipe");
        } else if (scriptFile == null && scriptUrl == null) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("File or the url must be provided for recipe");
        }

        try {
            RecipeRequest recipeRequest = new RecipeRequest();
            recipeRequest.setRecipeType(recipeType);
            recipeRequest.setName(name);
            recipeRequest.setDescription(description);
            recipeRequest.setUri(scriptUrl);

            if (scriptFile != null) {
                String script = IOUtils.toString(new FileInputStream(scriptFile));
                String encodedContent = Base64.encodeBase64String(script.getBytes());
                recipeRequest.setContent(encodedContent);
            }

            Long id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPublic(recipeRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPrivate(recipeRequest).getId();
            }
            return String.format(CREATE_SUCCESS_MESSAGE, id, recipeRequest.getName());
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"recipe show --id", "recipe show --name"})
    @Override
    public boolean showAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String show(Long id, String name, OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            RecipeResponse recipeMap;
            if (id != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint().get(id);
            } else if (name != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint().getPublic(name);
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException("Recipe not specified");
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", recipeMap.getId().toString());
            map.put("name", recipeMap.getName());
            map.put("description", recipeMap.getDescription());

            return shellContext.outputTransformer().render(outPutType, map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "recipe show --id", help = "Show the recipe by its id")
    @Override
    public String showById(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @CliCommand(value = "recipe show --name", help = "Show the recipe by its name")
    @Override
    public String showByName(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(null, name, outPutType);
    }

    @CliAvailabilityIndicator({"recipe delete --id", "recipe delete --name"})
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

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
            throw shellContext.exceptionTransformer().transformToRuntimeException("Recipe not specified (select recipe by --id or --name)");
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

}
