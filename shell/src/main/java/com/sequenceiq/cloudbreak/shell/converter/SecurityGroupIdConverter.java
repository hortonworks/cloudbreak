package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

public class SecurityGroupIdConverter extends AbstractConverter<SecurityGroupId> {

    @Autowired
    private CloudbreakContext context;

    public SecurityGroupIdConverter() {

    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return SecurityGroupId.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getSecurityGroups().keySet());
        } catch (Exception e) {
            return false;
        }
    }
}
