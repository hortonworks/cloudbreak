package com.sequenceiq.cloudbreak.converter

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse
import com.sequenceiq.cloudbreak.domain.SssdConfig

class SssdConfigToJsonConverterTest : AbstractEntityConverterTest<SssdConfig>() {

    private var underTest: SssdConfigToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = SssdConfigToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("id"))
    }

    override fun createSource(): SssdConfig {
        return TestUtil.sssdConfigs(1).iterator().next()
    }
}
