package com.sequenceiq.cloudbreak.controller.validation

import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import javax.validation.ConstraintTarget
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import javax.validation.metadata.ConstraintDescriptor

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import org.hibernate.validator.internal.engine.path.PathImpl

internal abstract class AbstractValidatorTest {

    val constraintViolationBuilder: ConstraintValidatorContext.ConstraintViolationBuilder
        get() = ConstraintValidatorContextImpl(
                ArrayList<String>(), null,
                PathImpl.createRootPath(),
                DummyConstraintDescriptor()).buildConstraintViolationWithTemplate("dummytemplate")

    private inner class DummyAnnotation : Annotation {

        override fun equals(obj: Any?): Boolean {
            return false
        }

        override fun hashCode(): Int {
            return 0
        }

        override fun toString(): String {
            return "dummy"
        }

        override fun annotationType(): Class<out Annotation> {
            return javaClass
        }
    }

    private inner class DummyConstraintDescriptor : ConstraintDescriptor<DummyAnnotation>, Serializable {

        override fun getAnnotation(): DummyAnnotation? {
            return null
        }

        override fun getMessageTemplate(): String {
            return ""
        }

        override fun getGroups(): Set<Class<*>> {
            return HashSet()
        }

        override fun getPayload(): Set<Class<out Payload>> {
            return HashSet()
        }

        override fun getValidationAppliesTo(): ConstraintTarget {
            return ConstraintTarget.PARAMETERS
        }

        override fun getConstraintValidatorClasses(): List<Class<out ConstraintValidator<DummyAnnotation, *>>> {
            return ArrayList()
        }

        override fun getAttributes(): Map<String, Any> {
            return HashMap()
        }

        override fun getComposingConstraints(): Set<ConstraintDescriptor<*>> {
            return HashSet()
        }

        override fun isReportAsSingleViolation(): Boolean {
            return false
        }
    }
}
