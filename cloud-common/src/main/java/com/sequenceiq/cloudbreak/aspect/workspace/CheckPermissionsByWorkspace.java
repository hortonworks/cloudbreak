package com.sequenceiq.cloudbreak.aspect.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.cloudbreak.authorization.ResourceAction;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionsByWorkspace {

    ResourceAction action() default ResourceAction.READ;

    int workspaceIndex();
}
