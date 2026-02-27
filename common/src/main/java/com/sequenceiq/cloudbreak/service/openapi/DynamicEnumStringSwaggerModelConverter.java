package com.sequenceiq.cloudbreak.service.openapi;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.common.api.AllowedEnumValuesAsStrings;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

public class DynamicEnumStringSwaggerModelConverter implements ModelConverter {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DynamicEnumStringSwaggerModelConverter.class);

    @Override
    public Schema<String> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema rawSchema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        try {
            Schema<String> schema = rawSchema instanceof Schema<?> ? (Schema<String>) rawSchema : null;

            if (schema != null && type.getCtxAnnotations() != null) {
                for (Annotation annotation : type.getCtxAnnotations()) {
                    if (annotation instanceof AllowedEnumValuesAsStrings enumAnnotation) {
                        Class<? extends Enum<?>> enumClass = enumAnnotation.value();
                        List<String> enumNames = Arrays.stream(enumClass.getEnumConstants())
                                .map(Enum::name)
                                .collect(Collectors.toList());
                        schema.setEnum(enumNames);
                        break;
                    }
                }
            }
            return schema;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve enum values for type: {}", type.getType(), e);
        }
        return rawSchema;
    }
}
