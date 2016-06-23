package com.sequenceiq.cloudbreak.converter

import org.mockito.BDDMockito.given
import org.mockito.Matchers.any

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

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.domain.SecurityGroup
import com.sequenceiq.cloudbreak.domain.SecurityRule

class SecurityGroupToJsonConverterTest : AbstractEntityConverterTest<SecurityGroup>() {

    @InjectMocks
    private var underTest: SecurityGroupToJsonConverter? = null

    @Mock
    private val conversionService: ConversionService? = null

    @Before
    fun setUp() {
        underTest = SecurityGroupToJsonConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(conversionService!!.convert(any<Any>(Any::class.java), any<TypeDescriptor>(TypeDescriptor::class.java), any<TypeDescriptor>(TypeDescriptor::class.java))).willReturn(ArrayList<SecurityRuleJson>())
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("owner", "account"))
    }

    override fun createSource(): SecurityGroup {
        return TestUtil.securityGroup(HashSet<SecurityRule>())
    }
}
