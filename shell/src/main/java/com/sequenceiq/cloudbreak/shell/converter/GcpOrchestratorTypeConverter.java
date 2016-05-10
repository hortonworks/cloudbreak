package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.GcpOrchestratorType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class GcpOrchestratorTypeConverter extends AbstractConverter<GcpOrchestratorType> {

    @Autowired
    private ShellContext context;

    public GcpOrchestratorTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return GcpOrchestratorType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getOrchestratorNamesByPlatform("GCP"));
        } catch (Exception e) {
            return false;
        }
    }
}
