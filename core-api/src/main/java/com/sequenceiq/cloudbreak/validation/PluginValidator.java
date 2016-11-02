package com.sequenceiq.cloudbreak.validation;

import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;

public class PluginValidator implements ConstraintValidator<ValidPlugin, Set<String>> {

    private static final String URL_PATTERN = "^(consul|base64)://.*";

    @Override
    public void initialize(ValidPlugin validPlugin) {
    }

    @Override
    public boolean isValid(Set<String> plugins, ConstraintValidatorContext cxt) {
        if (plugins == null || plugins.isEmpty()) {
            return true;
        }
        for (String plugin : plugins) {
            if (!plugin.matches(URL_PATTERN)) {
                return false;
            }
            if (plugin.startsWith("base64://") && !isValidBase64Plugin(plugin.replaceFirst("base64://", ""))) {
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
        return !content.isEmpty();
    }
}