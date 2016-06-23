package com.sequenceiq.cloudbreak.converter

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.domain.Recipe

class RecipeToJsonConverterTest : AbstractEntityConverterTest<Recipe>() {

    private var underTest: RecipeToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = RecipeToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("id"))
    }

    override fun createSource(): Recipe {
        return TestUtil.recipes(1).iterator().next()
    }
}
