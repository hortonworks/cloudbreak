package com.sequenceiq.cloudbreak.aspect.organization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sequenceiq.cloudbreak.authorization.OrganizationResource;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OrganizationResourceType {

    OrganizationResource resource();
}