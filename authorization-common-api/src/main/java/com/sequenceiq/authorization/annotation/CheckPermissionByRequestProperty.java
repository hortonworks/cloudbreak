package com.sequenceiq.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

@Repeatable(value = CheckPermissionByCompositeRequestProperty.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionByRequestProperty {

    AuthorizationVariableType type();

    AuthorizationResourceAction action();

    boolean skipOnNull() default false;

    CrnResourceDescriptor crnFilter() default CrnResourceDescriptor.UNKNOWN;

    String path();
}
