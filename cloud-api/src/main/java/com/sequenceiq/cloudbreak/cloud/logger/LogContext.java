package com.sequenceiq.cloudbreak.cloud.logger;


import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Indicates that a class or a method needs to be added to our log context
 * by adding the known type of class to the MDC log context.
 */
@Target(value = { ElementType.METHOD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
public @interface LogContext { }
