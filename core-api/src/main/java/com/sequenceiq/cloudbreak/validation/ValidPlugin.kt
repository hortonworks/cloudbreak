package com.sequenceiq.cloudbreak.validation

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Documented
@Constraint(validatedBy = PluginValidator::class)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class ValidPlugin(val message:

                             String = "Only plugins from http, https, git and consul protocols are allowed and base64 encoded plugins with base64:// prefix.", val groups: Array<KClass<*>> = arrayOf(), val payload: Array<KClass<out Payload>> = arrayOf())
