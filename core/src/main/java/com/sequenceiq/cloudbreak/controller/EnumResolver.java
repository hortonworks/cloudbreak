package com.sequenceiq.cloudbreak.controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiModel;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;

public class EnumResolver extends ModelResolver {
    public EnumResolver(ObjectMapper mapper) {
        super(mapper);
    }

    public EnumResolver() {
        super(Json.mapper());
    }

    @Override
    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> chain) {
        if (isEnumAnApiModel(type)) {
            String name = findName(type);
            // ask context to resolver enum type (for adding model definition
            // for enum under definitions section
            context.resolve(type);
            return new RefProperty(name);
        }
        return chain.next().resolveProperty(type, context, annotations, chain);
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        JavaType javaType = Json.mapper().constructType(type);
        if (javaType.isEnumType()) {
            ModelImpl model = new ModelImpl();
            Class<?> rawClass = javaType.getRawClass();
            ApiModel annotation = rawClass.getAnnotation(ApiModel.class);
            String name = annotation.value();
            if (name.isEmpty()) {
                name = rawClass.getSimpleName();
            }
            model.setName(name);
            model.setDescription(annotation.description());
            model.setType(StringProperty.TYPE);

            List<String> constants = findEnumConstants(rawClass);
            model.setEnum(constants);
            return model;
        }
        return chain.next().resolve(type, context, chain);
    }

    private String findName(Type type) {
        JavaType javaType = _mapper.constructType(type);
        Class<?> rawClass = javaType.getRawClass();
        ApiModel annotation = rawClass.getAnnotation(ApiModel.class);
        String name = annotation.value();
        if (name.isEmpty()) {
            name = rawClass.getSimpleName();
        }
        return name;
    }

    private boolean isEnumAnApiModel(Type type) {
        JavaType javaType = _mapper.constructType(type);
        return javaType.isEnumType()
                && javaType.getRawClass().isAnnotationPresent(ApiModel.class);
    }

    private List<String> findEnumConstants(Class<?> rawClass) {
        StringProperty p = new StringProperty();
        _addEnumProps(rawClass, p);
        return p.getEnum();
    }
}
