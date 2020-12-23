package com.sequenceiq.it.cloudbreak;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Retention(RetentionPolicy.RUNTIME)
@Component
@Scope("prototype")
public @interface Prototype {
}
