package com.sequenceiq.cloudbreak.controller.validation;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class TrustedPluginValidator implements ConstraintValidator<TrustedPlugin, Set<String>> {

    private static final String PLUGIN_URL_PATTERN = "https://github.com/.*/(consul-plugins-)[a-z0-9-._]*.git";

    private static final String TRUST_ALL = "all-accounts";

    @Value("${cb.plugins.trusted.sources:all-accounts}")
    private String trustedSources;

    @Override
    public void initialize(TrustedPlugin trustedPlugin) {
    }

    @Override
    public boolean isValid(Set<String> urls, ConstraintValidatorContext cxt) {
        for (String url : urls) {
            if (!url.matches(PLUGIN_URL_PATTERN)) {
                return false;
            } else {
                String source = url.replace("https://github.com/", "").split("/")[0];
                Set<String> trustedAccounts = parseTrustedSources(trustedSources);
                if (trustedAccounts.contains(TRUST_ALL)) {
                    return true;
                }
                if (!trustedAccounts.contains(source)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<String> parseTrustedSources(String trustedSources) {
        return new HashSet<>(Arrays.asList(trustedSources.split(":")));
    }

}