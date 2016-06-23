package com.sequenceiq.cloudbreak.cloud.logger


import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Indicates that a class or a method needs to be added to our log context
 * by adding the known type of class to the MDC log context.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
annotation class LogContext
