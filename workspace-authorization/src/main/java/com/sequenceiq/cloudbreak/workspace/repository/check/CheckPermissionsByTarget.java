package com.sequenceiq.cloudbreak.workspace.repository.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionsByTarget {

    ResourceAction action() default ResourceAction.READ;

    int targetIndex();
}
