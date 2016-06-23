package com.sequenceiq.cloudbreak.controller.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

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
import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.validation.UpdateStackRequestValidator

class UpdateStackRequestValidatorTest {

    private var underTest: UpdateStackRequestValidator? = null

    private var constraintValidatorContext: ConstraintValidatorContext? = null

    @Before
    fun setUp() {
        underTest = UpdateStackRequestValidator()
        constraintValidatorContext = ConstraintValidatorContextImpl(
                ArrayList<String>(), null,
                PathImpl.createRootPath(),
                DummyConstraintDescriptor())
    }

    @Test
    fun testIsValidShouldReturnTrueWhenStatusIsUpdated() {
        val updateStackJson = UpdateStackJson()
        updateStackJson.instanceGroupAdjustment = null
        updateStackJson.status = StatusRequest.STARTED
        val valid = underTest!!.isValid(updateStackJson, constraintValidatorContext)
        assertTrue(valid)
    }

    @Test
    fun testIsValidShouldReturnTrueWhenNodeCountIsUpdated() {
        val updateStackJson = UpdateStackJson()
        val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
        instanceGroupAdjustmentJson.scalingAdjustment = 12
        instanceGroupAdjustmentJson.instanceGroup = "slave_1"
        updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
        updateStackJson.status = null
        val valid = underTest!!.isValid(updateStackJson, constraintValidatorContext)
        assertTrue(valid)
    }

    @Test
    fun testInValidShouldReturnTrueWhenNodeCountIsLowerThanOneUpdatedAndWithClusterEvent() {
        val updateStackJson = UpdateStackJson()
        val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
        instanceGroupAdjustmentJson.scalingAdjustment = -1
        instanceGroupAdjustmentJson.withClusterEvent = true
        instanceGroupAdjustmentJson.instanceGroup = "slave_1"
        updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
        updateStackJson.status = null
        val valid = underTest!!.isValid(updateStackJson, constraintValidatorContext)
        assertFalse(valid)
    }

    @Test
    fun testIsValidShouldReturnFalseWhenRequestContainsNodeCountAndStatus() {
        val updateStackJson = UpdateStackJson()
        val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
        instanceGroupAdjustmentJson.scalingAdjustment = 4
        instanceGroupAdjustmentJson.instanceGroup = "slave_1"
        updateStackJson.status = StatusRequest.STARTED
        updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
        val valid = underTest!!.isValid(updateStackJson, constraintValidatorContext)
        assertFalse(valid)
    }

    @Test
    fun testIsValidShouldReturnFalseWhenRequestContainsOnlyNulls() {

        val updateStackJson = UpdateStackJson()
        updateStackJson.instanceGroupAdjustment = null
        updateStackJson.status = null
        val valid = underTest!!.isValid(updateStackJson, constraintValidatorContext)
        assertFalse(valid)
    }

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