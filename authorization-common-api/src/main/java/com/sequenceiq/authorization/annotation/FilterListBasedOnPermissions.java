package com.sequenceiq.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FilterListBasedOnPermissions {
    AuthorizationResourceAction action();
}
