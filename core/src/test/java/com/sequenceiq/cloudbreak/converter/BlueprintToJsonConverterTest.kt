package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.mockito.BDDMockito.given
import org.mockito.Matchers.anyString

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse

class BlueprintToJsonConverterTest : AbstractEntityConverterTest<Blueprint>() {

    @InjectMocks
    private var underTest: BlueprintToJsonConverter? = null

    @Mock
    private val jsonHelper: JsonHelper? = null

    @Mock
    private val jsonNode: JsonNode? = null

    @Before
    fun setUp() {
        underTest = BlueprintToJsonConverter()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testConvert() {
        // GIVEN
        given(jsonHelper!!.createJsonFromString(anyString())).willReturn(jsonNode)
        given(jsonNode!!.toString()).willReturn("dummyAmbariBlueprint")
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals("multi-node-yarn", result.blueprintName)
        assertAllFieldsNotNull(result)
    }

    @Test
    fun testConvertThrowsException() {
        // GIVEN
        given(jsonHelper!!.createJsonFromString(anyString())).willThrow(RuntimeException("error"))
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals("\"error\"", result.ambariBlueprint)
        assertAllFieldsNotNull(result)
    }

    override fun createSource(): Blueprint {
        return TestUtil.blueprint()
    }
}
