package com.sequenceiq.authorization.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ResourceObjectField {
    AuthorizationResourceAction action();

    AuthorizationResourceType type();

    AuthorizationVariableType variableType();
}
