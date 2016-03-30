package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AzureInstanceTypeConverter extends AbstractConverter<AzureInstanceType> {
    @Autowired
    private ShellContext context;

    public AzureInstanceTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return AzureInstanceType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getInstanceTypeNamesByPlatform("AZURE_RM"));
        } catch (Exception e) {
            return false;
        }
    }
}
