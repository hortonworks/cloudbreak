package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

class JsonToCloudbreakEventConverterTest : AbstractJsonConverterTest<CloudbreakEventsJson>() {

    private var underTest: JsonToCloudbreakEventConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToCloudbreakEventConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("event/cloudbreak-event.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }


    override val requestClass: Class<CloudbreakEventsJson>
        get() = CloudbreakEventsJson::class.java
}
