package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class GcpInstanceTypeConverter extends AbstractConverter<GcpInstanceType> {
    @Autowired
    private ShellContext context;

    public GcpInstanceTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return GcpInstanceType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getInstanceTypeNamesByPlatform("GCP"));
        } catch (Exception e) {
            return false;
        }
    }
}
