package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class InstanceGroupTemplateIdConverter extends AbstractConverter<InstanceGroupTemplateId> {

    @Autowired
    private ShellContext context;

    public InstanceGroupTemplateIdConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String s) {
        return InstanceGroupTemplateId.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getActiveTemplates());
        } catch (Exception e) {
            return false;
        }
    }
}
