package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.controller.json.PlatformVariantsJson;

public class PlatformVariantsToJsonConverterTest extends AbstractEntityConverterTest<PlatformVariants> {

    private static final String PLATFORM = "PLATFORM";
    private static final String VARIANT_1 = "VARIANT1";
    private static final String VARIANT_2 = "VARIANT2";

    private PlatformVariantsToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new PlatformVariantsToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        PlatformVariantsJson result = underTest.convert(getSource());
        // THEN
        assertTrue(result.getPlatformToVariants().get(PLATFORM).contains(VARIANT_1));
        assertEquals(VARIANT_2, result.getDefaultVariants().get(PLATFORM));
        assertAllFieldsNotNull(result);
    }



    @Override
    public PlatformVariants createSource() {
        Map<String, Collection<String>> platformToVariants = Maps.newHashMap();
        platformToVariants.put(PLATFORM, Arrays.asList(VARIANT_1, VARIANT_2));
        Map<String, String> defaultVariants = Maps.newHashMap();
        defaultVariants.put(PLATFORM, VARIANT_2);
        return new PlatformVariants(platformToVariants, defaultVariants);
    }
}
