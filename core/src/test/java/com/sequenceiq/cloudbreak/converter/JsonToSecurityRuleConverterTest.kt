package com.sequenceiq.cloudbreak.converter

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson
import com.sequenceiq.cloudbreak.domain.SecurityRule

class JsonToSecurityRuleConverterTest : AbstractJsonConverterTest<SecurityRuleJson>() {

    private var underTest: JsonToSecurityRuleConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToSecurityRuleConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("security-group/security-rule.json"))
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("securityGroup"))
    }

    override val requestClass: Class<SecurityRuleJson>
        get() = SecurityRuleJson::class.java
}
