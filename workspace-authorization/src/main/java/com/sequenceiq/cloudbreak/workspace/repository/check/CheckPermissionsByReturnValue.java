package com.sequenceiq.cloudbreak.workspace.repository.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.authorization.resource.ResourceAction;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckPermissionsByReturnValue {

    ResourceAction action() default ResourceAction.READ;
}
