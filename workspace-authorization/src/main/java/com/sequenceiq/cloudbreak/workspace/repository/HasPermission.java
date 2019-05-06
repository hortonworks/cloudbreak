package com.sequenceiq.cloudbreak.workspace.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface HasPermission {

    PermissionType permission() default PermissionType.READ;

    int targetIndex() default -1;

    ConditionType condition() default ConditionType.POST;
}
