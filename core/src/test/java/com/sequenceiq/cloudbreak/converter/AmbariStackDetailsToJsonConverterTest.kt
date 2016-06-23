package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson

class AmbariStackDetailsToJsonConverterTest : AbstractEntityConverterTest<AmbariStackDetails>() {

    private var underTest: AmbariStackDetailsToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = AmbariStackDetailsToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals("dummyOs", result.os)
        assertAllFieldsNotNull(result)
    }

    override fun createSource(): AmbariStackDetails {
        return TestUtil.ambariStackDetails()
    }
}
