package com.sequenceiq.cloudbreak.controller.validation;

import static com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.RECIPE_KEY_PREFIX;

import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;

import com.sequenceiq.cloudbreak.domain.PluginExecutionType;

public class PluginValidator implements ConstraintValidator<ValidPlugin, Map<String, PluginExecutionType>> {

    private static final String URL_PATTERN = "^(http|https|git|consul|base64)://.*";
    private static final String CONSUL_PATTERN = "^consul://" + RECIPE_KEY_PREFIX + "([a-z][-a-z0-9]*[a-z0-9])";
    private static final String SCRIPT_PATTERN = "^(recipe-pre-install|recipe-post-install):.*";

    @Override
    public void initialize(ValidPlugin validPlugin) {
    }

    @Override
    public boolean isValid(Map<String, PluginExecutionType> plugins, ConstraintValidatorContext cxt) {
        if (plugins == null || plugins.isEmpty()) {
            return false;
        }
        for (String url : plugins.keySet()) {
            if (!url.matches(URL_PATTERN)) {
                return false;
            } else if (url.startsWith("consul://") && !url.matches(CONSUL_PATTERN)) {
                return false;
            } else if (url.startsWith("base64://") && !isValidBase64Plugin(url.replaceFirst("base64://", ""))) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBase64Plugin(String pluginContent) {
        if (!Base64.isBase64(pluginContent)) {
            return false;
        }
        String content = new String(Base64.decodeBase64(pluginContent));
        if (content.isEmpty()) {
            return false;
        }

        boolean tomlFound = false;
        boolean scriptFound = false;
        for (String line : content.split("\n")) {
            if (line.startsWith("plugin.toml:")) {
                tomlFound = true;
            } else if (line.matches(SCRIPT_PATTERN)) {
                scriptFound = true;
            }
        }

        return tomlFound && scriptFound;
    }
}