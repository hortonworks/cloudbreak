package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson

class JsonToAmbariStackDetailsConverterTest : AbstractJsonConverterTest<AmbariStackDetailsJson>() {

    private var underTest: JsonToAmbariStackDetailsConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToAmbariStackDetailsConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/ambari-stack-details.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<AmbariStackDetailsJson>
        get() = AmbariStackDetailsJson::class.java
}
