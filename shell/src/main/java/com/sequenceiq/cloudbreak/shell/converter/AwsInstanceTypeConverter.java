package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.AwsInstanceType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AwsInstanceTypeConverter extends AbstractConverter<AwsInstanceType> {

    @Autowired
    private ShellContext context;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return AwsInstanceType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getInstanceTypeNamesByPlatform("AWS"));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
