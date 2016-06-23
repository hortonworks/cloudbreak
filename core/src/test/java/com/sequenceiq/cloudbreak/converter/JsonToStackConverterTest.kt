package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation
import com.sequenceiq.cloudbreak.domain.FailurePolicy
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Orchestrator
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.stack.StackParameterService

class JsonToStackConverterTest : AbstractJsonConverterTest<StackRequest>() {

    @InjectMocks
    private var underTest: JsonToStackConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Mock
    private val stackParameterService: StackParameterService? = null

    @Before
    fun setUp() {
        underTest = JsonToStackConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        val instanceGroup = mock<InstanceGroup>(InstanceGroup::class.java)
        `when`(instanceGroup.instanceGroupType).thenReturn(InstanceGroupType.GATEWAY)

        // GIVEN
        given(conversionService!!.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(HashSet(Arrays.asList(instanceGroup)))
        given(conversionService.convert<Any>(any<Any>(Any::class.java), any<Class>(Class<Any>::class.java))).willReturn(FailurePolicy()).willReturn(Orchestrator())
        given(stackParameterService!!.getStackParams(any<StackRequest>(StackRequest::class.java))).willReturn(ArrayList<StackParamValidation>())
        // WHEN
        val stack = underTest!!.convert(getRequest("stack/stack.json"))
        // THEN
        assertAllFieldsNotNull(stack, Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort",
                "template", "network", "securityConfig", "securityGroup", "version", "created", "platformVariant", "cloudPlatform"))
    }

    override val requestClass: Class<StackRequest>
        get() = StackRequest::class.java
}
