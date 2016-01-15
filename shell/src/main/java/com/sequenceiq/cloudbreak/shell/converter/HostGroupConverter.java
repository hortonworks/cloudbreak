package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

public class HostGroupConverter extends AbstractConverter<HostGroup> {

    @Autowired
    private CloudbreakContext context;

    public HostGroupConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String s) {
        return HostGroup.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getActiveHostGroups());
        } catch (Exception e) {
            return false;
        }
    }
}
