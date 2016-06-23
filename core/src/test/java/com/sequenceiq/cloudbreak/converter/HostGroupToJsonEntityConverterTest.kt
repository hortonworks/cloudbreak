package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Matchers.any
import org.mockito.Mockito.`when`

import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.core.convert.ConversionService

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.Recipe

class HostGroupToJsonEntityConverterTest : AbstractEntityConverterTest<HostGroup>() {

    @InjectMocks
    private var underTest: HostGroupToJsonConverter? = null
    @Mock
    private val conversionService: ConversionService? = null

    @Before
    fun setUp() {
        underTest = HostGroupToJsonConverter()
        MockitoAnnotations.initMocks(this)
        `when`(conversionService!!.convert<Any>(any<Class>(Class<Any>::class.java), any<Class>(Class<Any>::class.java))).thenReturn(ConstraintJson())
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1, result.metadata.size.toLong())
        assertTrue(result.recipeIds.contains(1L))
        assertEquals("dummyName", result.name)
        assertAllFieldsNotNull(result)
    }

    @Test
    fun testConvertWithoutRecipes() {
        // GIVEN
        source.recipes = HashSet<Recipe>()
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertEquals(1, result.metadata.size.toLong())
        assertFalse(result.recipeIds.contains(1L))
        assertEquals("dummyName", result.name)
        assertAllFieldsNotNull(result)
    }


    override fun createSource(): HostGroup {
        return TestUtil.hostGroup()
    }
}
