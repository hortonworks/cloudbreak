package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.common.type.RecipeExecutionPhase;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class JsonToRecipeConverter extends AbstractConversionServiceAwareConverter<RecipeRequest, Recipe> {
    @Override
    public Recipe convert(RecipeRequest json) {
        Recipe recipe = new Recipe();
        recipe.setName(json.getName());
        recipe.setDescription(json.getDescription());
        recipe.setKeyValues(json.getProperties());
        Set<Plugin> plugins = new HashSet<>();
        Set<String> originalPlugins = json.getPlugins();
        if (originalPlugins != null && !originalPlugins.isEmpty()) {
            plugins = originalPlugins.stream().map(Plugin::new).collect(Collectors.toSet());
        }
        if (json.getPreUrl() != null) {
            setScriptContent(plugins, json.getPreUrl(), "recipe-pre-install");
            recipe.addKeyValue(RecipeExecutionPhase.PRE.url(), json.getPreUrl());
        }
        if (json.getPostUrl() != null) {
            setScriptContent(plugins, json.getPostUrl(), "recipe-post-install");
            recipe.addKeyValue(RecipeExecutionPhase.POST.url(), json.getPostUrl());
        }
        if (plugins.isEmpty()) {
            throw new BadRequestException("At least one script is required");
        }
        recipe.setPlugins(plugins);
        return recipe;
    }

    private void setScriptContent(Set<Plugin> plugins, String url, String name) {
        try {
            String script = URLUtils.readUrl(url);
            for (Plugin plugin : plugins) {
                String decodedContent = new String(Base64.decodeBase64(plugin.getContent().replaceFirst("base64://", "")));
                if (decodedContent.contains(name)) {
                    decodedContent = decodedContent.replaceAll(name + ":.*\\n", "");
                }
                plugin.setContent("base64://"
                        + Base64.encodeBase64String((decodedContent + name + ":" + Base64.encodeBase64String(script.getBytes()) + "\n").getBytes()));
            }
            if (plugins.isEmpty()) {
                plugins.add(new Plugin("base64://" + Base64.encodeBase64String((name + ":" + Base64.encodeBase64String(script.getBytes()) + "\n").getBytes())));
            }
        } catch (IOException e) {
            throw new BadRequestException("Cannot download the" + name + " script from URL: " + url);
        }
    }
}
