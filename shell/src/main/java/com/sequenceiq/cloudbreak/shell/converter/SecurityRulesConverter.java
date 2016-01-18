package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;

public class SecurityRulesConverter extends AbstractConverter {

    public SecurityRulesConverter() {

    }

    @Override
    public boolean supports(Class type, String optionContext) {
        return SecurityRules.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List list, Class targetType, String existingData, String optionContext, MethodTarget target) {
        return false;
    }
}
