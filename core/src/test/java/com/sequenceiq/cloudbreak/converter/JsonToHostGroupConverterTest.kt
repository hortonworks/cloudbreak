package com.sequenceiq.cloudbreak.converter

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.domain.HostGroup

class JsonToHostGroupConverterTest : AbstractJsonConverterTest<HostGroupJson>() {

    private var underTest: JsonToHostGroupConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToHostGroupConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/host-group.json"))
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "instanceGroup", "constraint"))
    }

    override val requestClass: Class<HostGroupJson>
        get() = HostGroupJson::class.java
}
