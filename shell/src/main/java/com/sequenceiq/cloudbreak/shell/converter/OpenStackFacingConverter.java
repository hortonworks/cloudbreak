package com.sequenceiq.cloudbreak.shell.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.sequenceiq.cloudbreak.shell.completion.OpenStackFacing;

public class OpenStackFacingConverter extends AbstractConverter<OpenStackFacing> {

    private static Collection<String> values = Arrays.asList("admin", "public", "internal");

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return OpenStackFacing.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        return getAllPossibleValues(completions, values);
    }
}
