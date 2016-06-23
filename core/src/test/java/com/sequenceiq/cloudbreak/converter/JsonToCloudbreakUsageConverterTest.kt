package com.sequenceiq.cloudbreak.converter

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson

class JsonToCloudbreakUsageConverterTest : AbstractJsonConverterTest<CloudbreakUsageJson>() {

    private var underTest: JsonToCloudbreakUsageConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToCloudbreakUsageConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("usage/cloudbreak-usage.json"))
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("day", "costs"))
    }

    override val requestClass: Class<CloudbreakUsageJson>
        get() = CloudbreakUsageJson::class.java
}
