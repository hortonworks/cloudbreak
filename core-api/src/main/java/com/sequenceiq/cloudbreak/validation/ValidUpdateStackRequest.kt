package com.sequenceiq.cloudbreak.validation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UpdateStackRequestValidator::class)
annotation class ValidUpdateStackRequest(val message:

                                         String = "Update stack request is not valid.", val groups: Array<KClass<*>> = arrayOf(), val payload: Array<KClass<out Payload>> = arrayOf())