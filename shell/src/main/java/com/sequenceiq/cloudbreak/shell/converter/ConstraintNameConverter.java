package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.ConstraintName;
import com.sequenceiq.cloudbreak.shell.model.MarathonContext;

public class ConstraintNameConverter extends AbstractConverter<ConstraintName> {

    @Autowired
    private MarathonContext marathonContext;

    public ConstraintNameConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String s) {
        return ConstraintName.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, marathonContext.getConstraints());
        } catch (Exception e) {
            return false;
        }
    }
}
