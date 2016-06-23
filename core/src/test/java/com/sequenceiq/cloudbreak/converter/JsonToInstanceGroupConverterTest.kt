package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.anyLong

import java.util.Arrays

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.security.access.AccessDeniedException

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.service.template.TemplateService

class JsonToInstanceGroupConverterTest : AbstractJsonConverterTest<InstanceGroupJson>() {

    @InjectMocks
    private var underTest: JsonToInstanceGroupConverter? = null

    @Mock
    private val templateService: TemplateService? = null

    @Before
    fun setUp() {
        underTest = JsonToInstanceGroupConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(templateService!!.get(anyLong())).willReturn(TestUtil.gcpTemplate(51L))
        // WHEN
        val instanceGroup = underTest!!.convert(getRequest("stack/instance-group.json"))
        // THEN
        assertAllFieldsNotNull(instanceGroup, Arrays.asList("stack"))
    }

    @Test(expected = AccessDeniedException::class)
    fun testConvertWhenAccessDenied() {
        // GIVEN
        given(templateService!!.get(anyLong())).willThrow(AccessDeniedException("exception"))
        // WHEN
        underTest!!.convert(getRequest("stack/instance-group.json"))
    }

    override val requestClass: Class<InstanceGroupJson>
        get() = InstanceGroupJson::class.java
}
