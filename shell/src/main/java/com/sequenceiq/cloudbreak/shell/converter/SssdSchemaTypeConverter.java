package com.sequenceiq.cloudbreak.shell.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.MethodTarget;

import com.google.common.collect.Collections2;
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType;

public class SssdSchemaTypeConverter extends AbstractConverter<SssdSchemaType> {

    private static Collection<String> values;

    {
        values = Collections2.transform(Arrays.asList(com.sequenceiq.cloudbreak.api.model.SssdSchemaType.values()), Enum::name);
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return SssdSchemaType.class.isAssignableFrom(type);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        return getAllPossibleValues(completions, values);
    }
}
