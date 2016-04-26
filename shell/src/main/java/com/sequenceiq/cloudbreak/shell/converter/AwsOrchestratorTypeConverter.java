package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.AwsOrchestratorType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AwsOrchestratorTypeConverter extends AbstractConverter<AwsOrchestratorType> {

    @Autowired
    private ShellContext context;

    public AwsOrchestratorTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return AwsOrchestratorType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getOrchestratorNamesByPlatform("AWS"));
        } catch (Exception e) {
            return false;
        }
    }
}
