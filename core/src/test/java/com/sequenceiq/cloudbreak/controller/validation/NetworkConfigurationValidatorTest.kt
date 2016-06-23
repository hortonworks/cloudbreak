package com.sequenceiq.cloudbreak.controller.validation

import org.junit.Assert.assertTrue

import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import javax.validation.ConstraintTarget
import javax.validation.ConstraintValidator
import javax.validation.Payload
import javax.validation.metadata.ConstraintDescriptor

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator

@RunWith(MockitoJUnitRunner::class)
class NetworkConfigurationValidatorTest {

    @InjectMocks
    private val underTest: NetworkConfigurationValidator? = null

    @Test
    fun validNetworkRequestReturnTrue() {
        assertTrue(underTest!!.validateNetworkForStack(TestUtil.network(), TestUtil.generateGcpInstanceGroupsByNodeCount(1, 2, 3)))
    }

    @Test(expected = BadRequestException::class)
    fun inValidNetworkRequestReturnFalse() {
        underTest!!.validateNetworkForStack(TestUtil.network("10.0.0.1/32"), TestUtil.generateGcpInstanceGroupsByNodeCount(10000, 10000, 10000))
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