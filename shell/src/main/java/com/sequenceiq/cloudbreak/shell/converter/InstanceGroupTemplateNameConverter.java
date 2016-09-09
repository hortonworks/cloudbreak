package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class InstanceGroupTemplateNameConverter extends AbstractConverter<InstanceGroupTemplateName> {

    @Autowired
    private ShellContext context;

    public InstanceGroupTemplateNameConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String s) {
        return InstanceGroupTemplateName.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getActiveTemplateNames());
        } catch (Exception e) {
            return false;
        }
    }
}
