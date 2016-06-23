package com.sequenceiq.cloudbreak.converter

import org.junit.Before
import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.domain.Recipe

class JsonToRecipeConverterTest : AbstractJsonConverterTest<RecipeRequest>() {

    private var underTest: JsonToRecipeConverter? = null

    @Before
    fun setUp() {
        underTest = JsonToRecipeConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(getRequest("stack/recipe.json"))
        // THEN
        assertAllFieldsNotNull(result)
    }

    override val requestClass: Class<RecipeRequest>
        get() = RecipeRequest::class.java
}
