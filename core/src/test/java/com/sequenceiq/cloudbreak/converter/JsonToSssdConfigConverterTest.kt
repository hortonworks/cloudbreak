package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest
import com.sequenceiq.cloudbreak.domain.SssdConfig

class JsonToSssdConfigConverterTest : AbstractJsonConverterTest<SssdConfigRequest>() {

    private var underTest: JsonToSssdConfigConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToSssdConfigConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/sssd_config.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<SssdConfigRequest>
        get() = SssdConfigRequest::class.java
}
