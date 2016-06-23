package com.sequenceiq.cloudbreak.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import java.util.Arrays

import org.junit.Before
import org.junit.Test

import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson

class PlatformVariantsToJsonConverterTest : AbstractEntityConverterTest<PlatformVariants>() {

    private var underTest: PlatformVariantsToJsonConverter? = null

    @Before
    fun setUp() {
        underTest = PlatformVariantsToJsonConverter()
    }

    @Test
    fun testConvert() {
        // GIVEN
        // WHEN
        val result = underTest!!.convert(source)
        // THEN
        assertTrue(result.platformToVariants[PLATFORM.value()].contains(VARIANT_1.value()))
        assertEquals(VARIANT_2.value(), result.defaultVariants[PLATFORM.value()])
        assertAllFieldsNotNull(result)
    }


    override fun createSource(): PlatformVariants {
        val platformToVariants = Maps.newHashMap<Platform, Collection<Variant>>()
        platformToVariants.put(PLATFORM, Arrays.asList(VARIANT_1, VARIANT_2))
        val defaultVariants = Maps.newHashMap<Platform, Variant>()
        defaultVariants.put(PLATFORM, VARIANT_2)
        return PlatformVariants(platformToVariants, defaultVariants)
    }

    companion object {

        private val PLATFORM = Platform.platform("PLATFORM")
        private val VARIANT_1 = Variant.variant("VARIANT1")
        private val VARIANT_2 = Variant.variant("VARIANT2")
    }
}
