package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

class CloudbreakEventToJsonConverterTest : AbstractEntityConverterTest<CloudbreakEvent>() {
    private var underTest: CloudbreakEventToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = CloudbreakEventToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals("message", result.eventMessage)
        assertAllFieldsNotNull(result, Lists.newArrayList("availabilityZone"))
    }

    override fun createSource(): CloudbreakEvent {
        return TestUtil.gcpCloudbreakEvent(1L)
    }
}
