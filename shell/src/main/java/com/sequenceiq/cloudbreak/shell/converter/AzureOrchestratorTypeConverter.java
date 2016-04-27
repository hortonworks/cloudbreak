package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.ArmOrchestratorType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AzureOrchestratorTypeConverter extends AbstractConverter<ArmOrchestratorType> {

    @Autowired
    private ShellContext context;

    public AzureOrchestratorTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return ArmOrchestratorType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getOrchestratorNamesByPlatform("AZURE_RM"));
        } catch (Exception e) {
            return false;
        }
    }
}
