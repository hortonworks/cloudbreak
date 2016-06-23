package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any

import java.util.Arrays

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule

class JsonToSecurityGroupConverterTest : AbstractJsonConverterTest<SecurityGroupJson>() {

    @InjectMocks
    private var underTest: JsonToSecurityGroupConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Before
    fun setUp() {
        underTest = JsonToSecurityGroupConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(conversionService!!.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(Sets.newConcurrentHashSet(Arrays.asList(SecurityRule())))
        // WHEN
        val result = underTest!!.convert(getRequest("security-group/security-group.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<SecurityGroupJson>
        get() = SecurityGroupJson::class.java
}
