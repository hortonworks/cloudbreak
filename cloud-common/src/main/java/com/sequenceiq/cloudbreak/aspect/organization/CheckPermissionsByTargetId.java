package com.sequenceiq.cloudbreak.aspect.organization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionsByTargetId {

    Action action() default Action.READ;

    int targetIdIndex() default 0;
}
