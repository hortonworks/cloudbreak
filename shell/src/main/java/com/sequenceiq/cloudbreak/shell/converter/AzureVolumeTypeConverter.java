package com.sequenceiq.cloudbreak.shell.converter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.AzureVolumeType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

public class AzureVolumeTypeConverter extends AbstractConverter<AzureVolumeType> {

    @Autowired
    private CloudbreakContext context;

    public AzureVolumeTypeConverter() {
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return AzureVolumeType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        try {
            return getAllPossibleValues(completions, context.getVolumeTypesByPlatform("AZURE_RM"));
        } catch (Exception e) {
            return false;
        }
    }
}
