package com.sequenceiq.cloudbreak.controller.validation;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class TrustedPluginValidator implements ConstraintValidator<TrustedPlugin, String> {

    @Value("${cb.plugins.trusted.sources:sequenceiq}")
    private String trustedSources;

    @Override
    public void initialize(TrustedPlugin trustedPlugin) {
    }

    @Override
    public boolean isValid(String urlField, ConstraintValidatorContext cxt) {
        if (urlField == null) {
            return false;
        }
        if (urlField.matches("https://github.com/.*(consul-plugins-)[a-z]*.git")) {
            String source = urlField.replace("https://github.com/", "").split("/")[0];
            if (parseTrustedSources(trustedSources).contains(source)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> parseTrustedSources(String trustedSources) {
        return new HashSet<>(Arrays.asList(trustedSources.split(":")));
    }

}