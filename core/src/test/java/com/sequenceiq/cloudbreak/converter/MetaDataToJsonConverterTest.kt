package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.api.model.InstanceStatus

class MetaDataToJsonConverterTest : AbstractEntityConverterTest<InstanceMetaData>() {

    private var underTest: MetaDataToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = MetaDataToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals("test1", result.discoveryFQDN)
        assertTrue(result.ambariServer!!)
        assertAllFieldsNotNull(result)
    }

    override fun createSource(): InstanceMetaData {
        return TestUtil.instanceMetaData(1L, InstanceStatus.REGISTERED, true,
                TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, TestUtil.gcpTemplate(1L)))
    }
}
