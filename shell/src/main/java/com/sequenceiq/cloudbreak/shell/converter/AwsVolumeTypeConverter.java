package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.AwsVolumeType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AwsVolumeTypeConverter extends AbstractConverter<AwsVolumeType> {

    @Autowired
    private ShellContext context;

    public AwsVolumeTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return AwsVolumeType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getVolumeTypesByPlatform("AWS"));
        } catch (Exception e) {
            return false;
        }
    }
}
