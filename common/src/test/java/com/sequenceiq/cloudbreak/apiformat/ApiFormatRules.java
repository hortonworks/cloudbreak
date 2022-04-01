package com.sequenceiq.cloudbreak.apiformat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

public enum ApiFormatRules implements ApiFormatRule {

    HAS_JSON_IGNORE_UNKNOWN_ANNOTATION {
        @Override
        public Optional<String> apply(Class<?> modelClass) {
            if (hasAnnotation(modelClass, JsonIgnoreProperties.class, JsonIgnoreProperties::ignoreUnknown)) {
                return Optional.empty();
            }
            return validationError("missing @JsonIgnoreProperties(ignoreUnknown = true) annotation", this);
        }
    },
    HAS_API_MODEL_ANNOTATION {
        @Override
        public Optional<String> apply(Class<?> modelClass) {
            if (hasAnnotation(modelClass, ApiModel.class)) {
                return Optional.empty();
            }
            return validationError("missing @ApiModel annotation", this);
        }
    },
    HAS_JSON_INCLUDE_NON_NULL {
        @Override
        public Optional<String> apply(Class<?> modelClass) {
            if (hasAnnotation(modelClass, JsonInclude.class,
                    annotation -> JsonInclude.Include.NON_NULL.equals(annotation.value()))) {
                return Optional.empty();
            }
            return validationError("missing @JsonInclude(Include.NON_NULL) annotation", this);
        }
    },
    HAS_API_MODEL_PROPERTY_ON_FIELDS {
        @Override
        public Optional<String> apply(Class<?> modelClass) {
            List<String> fieldsWithoutAnnotation = new ArrayList<>();
            for (Field field : modelClass.getDeclaredFields()) {

                if (!Modifier.isStatic(field.getModifiers())
                        && (!field.isAnnotationPresent(ApiModelProperty.class)
                        || Strings.isNullOrEmpty(field.getAnnotation(ApiModelProperty.class).value()))) {
                    fieldsWithoutAnnotation.add(field.getName());
                }
            }
            if (fieldsWithoutAnnotation.isEmpty()) {
                return Optional.empty();
            } else {
                return validationError("missing @ApiModelProperty(<description>) annotation on properties: " + fieldsWithoutAnnotation, this);
            }
        }
    };

    private static <T extends Annotation> boolean hasAnnotation(Class<?> clazz, Class<T> annotation) {
        return clazz.isAnnotationPresent(annotation);
    }

    private static <T extends Annotation> boolean hasAnnotation(Class<?> clazz, Class<T> annotation, Predicate<T> annotationCondition) {
        return clazz.isAnnotationPresent(annotation) && annotationCondition.test(clazz.getAnnotation(annotation));
    }

    private static Optional<String> validationError(String message, ApiFormatRules rule) {
        return Optional.of(message + ", rule: " + rule.name());
    }
}
