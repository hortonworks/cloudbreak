package com.sequenceiq.cloudbreak.controller.validation;


import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.domain.PluginExecutionType;

public class PluginValidator implements ConstraintValidator<ValidPlugin, Map<String, PluginExecutionType>> {

    private static final String PLUGIN_URL_PATTERN = "^(http|https|git)://.*";

    @Override
    public void initialize(ValidPlugin validPlugin) {
    }

    @Override
    public boolean isValid(Map<String, PluginExecutionType> plugins, ConstraintValidatorContext cxt) {
        if (plugins == null || plugins.isEmpty()) {
            return false;
        }
        for (String url : plugins.keySet()) {
            if (!url.matches(PLUGIN_URL_PATTERN)) {
                return false;
            }
        }
        return true;
    }
}