package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.domain.FailurePolicy

class JsonToFailurePolicyConverterTest : AbstractJsonConverterTest<FailurePolicyJson>() {

    private var underTest: JsonToFailurePolicyConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToFailurePolicyConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/failure-policy.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    @Test
    fun testConvertWithoutThreshold() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/failure-policy-without-threshold.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<FailurePolicyJson>
        get() = FailurePolicyJson::class.java
}
