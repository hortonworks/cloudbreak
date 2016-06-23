package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest

class JsonToBlueprintConverterTest : AbstractJsonConverterTest<BlueprintRequest>() {

    @InjectMocks
    private var underTest: JsonToBlueprintConverter? = null

    @Mock
    private val jsonHelper: JsonHelper? = null

    @Before
    fun setUp() {
        underTest = JsonToBlueprintConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/blueprint.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<BlueprintRequest>
        get() = BlueprintRequest::class.java
}
