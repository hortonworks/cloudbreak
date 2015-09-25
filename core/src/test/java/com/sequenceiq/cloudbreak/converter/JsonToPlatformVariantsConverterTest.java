package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.controller.json.PlatformVariantsJson;

public class JsonToPlatformVariantsConverterTest extends AbstractJsonConverterTest<PlatformVariantsJson> {
    private static final String PLATFORM_1 = "PLATFORM_1";
    private static final String PLATFORM_2 = "PLATFORM_2";
    private static final String VARIANT_1 = "VARIANT_1";
    private static final String VARIANT_2 = "VARIANT_2";

    private JsonToPlatformVariantsConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToPlatformVariantsConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        PlatformVariants result = underTest.convert(getRequest("stack/platform-variant.json"));
        // THEN
        assertAllFieldsNotNull(result);
        assertTrue(result.getPlatformToVariants().get(PLATFORM_1).contains(VARIANT_1));
        assertTrue(result.getPlatformToVariants().get(PLATFORM_2).contains(VARIANT_1));
        assertTrue(result.getPlatformToVariants().get(PLATFORM_2).contains(VARIANT_2));
        assertEquals(VARIANT_1, result.getDefaultVariants().get(PLATFORM_1));
        assertEquals(VARIANT_2, result.getDefaultVariants().get(PLATFORM_2));
    }

    @Override
    public Class<PlatformVariantsJson> getRequestClass() {
        return PlatformVariantsJson.class;
    }
}
