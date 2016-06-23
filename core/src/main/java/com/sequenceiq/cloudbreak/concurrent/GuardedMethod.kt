package com.sequenceiq.cloudbreak.concurrent

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

import org.springframework.stereotype.Component

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class GuardedMethod(val lockPrefix: String = "")
